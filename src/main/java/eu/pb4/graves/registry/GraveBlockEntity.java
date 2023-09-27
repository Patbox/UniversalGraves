package eu.pb4.graves.registry;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.grave.GraveHolder;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.grave.PositionedItemStack;
import eu.pb4.graves.model.GraveModelHandler;
import eu.pb4.graves.other.VanillaInventoryMask;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static eu.pb4.graves.registry.AbstractGraveBlock.IS_LOCKED;

public class GraveBlockEntity extends AbstractGraveBlockEntity implements GraveHolder {
    public static BlockEntityType<GraveBlockEntity> BLOCK_ENTITY_TYPE;
    public BlockState replacedBlockState = Blocks.AIR.getDefaultState();
    private Grave data = null;
    private VisualGraveData visualData = VisualGraveData.DEFAULT;
    private long graveId = -1;
    private GraveModelHandler model;
    private Map<String, Text> cachedPlaceholders;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY_TYPE, pos, state);
    }

    public void setGrave(Grave grave, BlockState oldBlockState) {
        this.replacedBlockState = oldBlockState;
        this.setGrave(grave);
    }

    public void setGrave(Grave grave) {
        this.data = grave;

        if (grave != null) {
            this.visualData = grave.toVisualGraveData();
            this.graveId = grave.getId();
        } else {
            this.graveId = -1;
        }
        this.markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put("BlockState", NbtHelper.fromBlockState(this.replacedBlockState));
        nbt.put("VisualData", this.getClientData().toNbt());
        nbt.putLong("GraveId", this.graveId);
    }


    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        try {
            if (nbt.contains("GraveInfo", NbtElement.COMPOUND_TYPE)) {
                // Legacy grave handling
                this.data = new Grave();
                this.data.readNbt(nbt.getCompound("GraveInfo"));

                NbtList nbtList = nbt.getList("Items", NbtElement.COMPOUND_TYPE);

                for (NbtElement compound : nbtList) {
                    this.data.getItems().add(new PositionedItemStack(ItemStack.fromNbt((NbtCompound) compound), -1, VanillaInventoryMask.INSTANCE, null, Set.of()));
                }
                GraveManager.INSTANCE.add(this.data);
                this.visualData = this.data.toVisualGraveData();
            } else if (nbt.contains("GraveId", NbtElement.LONG_TYPE)) {
                this.graveId = nbt.getLong("GraveId");
            }

            if (this.data == null) {
                this.fetchGraveData();
            }

            if (this.visualData == null) {
                this.visualData = VisualGraveData.fromNbt(nbt.getCompound("VisualData"));
            }

            this.replacedBlockState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), (NbtCompound) Objects.requireNonNull(nbt.get("BlockState")));
        } catch (Exception e) {
            this.visualData = VisualGraveData.DEFAULT;
        }
    }

    protected void fetchGraveData() {
        this.data = GraveManager.INSTANCE.getId(this.graveId);

        if (this.data != null) {
            this.visualData = this.data.toVisualGraveData();
            this.updateForAllPlayers();
            this.markDirty();
        }
    }

    protected void updateForAllPlayers() {
        assert this.world != null;
    }


    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof GraveBlockEntity self) || world.isClient()) {
            return;
        }
        self.cachedPlaceholders = null;
        if (self.data == null) {
            if (world.getTime() % 10 == 0) {
                self.fetchGraveData();
            }
            return;
        }

        if (self.data.isRemoved()) {
            self.breakBlock();
            return;
        }

        if (self.model == null) {
            self.model = (GraveModelHandler) BlockBoundAttachment.get(world, pos).holder();
            self.model.setGrave(self);
        }

        if (world.getTime() % 5 != 0) {
            self.model.maybeTick(world.getTime());
            return;
        }

        Config config = ConfigManager.getConfig();


        if (config.protection.breakingTime > -1 && self.data.shouldNaturallyBreak()) {
            world.setBlockState(pos, self.replacedBlockState, Block.NOTIFY_ALL);
            return;
        }

        if (state.get(IS_LOCKED) && !self.data.isProtected()) {
            world.setBlockState(pos, state.with(IS_LOCKED, false));
            if (self.model != null) {
                self.model.updateModel();
            }
        }

        self.model.maybeTick(world.getTime());
    }

    @Override
    public void onModelChanged(String model) {
        if (this.model != null) {
            this.model.updateModel();
        }
    }

    public void breakBlock() {
        breakBlock(true);
    }

    public void breakBlock(boolean canCreateVisual) {
        assert world != null;
        if (canCreateVisual && ConfigManager.getConfig().placement.createVisualGrave) {
            world.setBlockState(pos, VisualGraveBlock.INSTANCE.getStateWithProperties(this.getCachedState()));

            if (world.getBlockEntity(pos) instanceof VisualGraveBlockEntity blockEntity) {
                blockEntity.setVisualData(this.getClientData(), this.replacedBlockState);
                blockEntity.setModelId(this.getGraveModelId());
            }
        } else {
            world.setBlockState(pos, this.replacedBlockState);
        }
    }


    @Nullable
    public Grave getGrave() {
        if (this.data == null) {
            this.fetchGraveData();
        }
        return this.data;
    }

    public VisualGraveData getClientData() {
        return this.data != null ? this.data.toVisualGraveData() : this.visualData;
    }

    @Override
    public void updateModel() {
        if (this.model != null) {
            this.model.updateModel();
        }
    }

    @Override
    public boolean isGraveProtected() {
        return this.getCachedState().get(IS_LOCKED);
    }

    @Override
    public boolean isGraveBroken() {
        return false;
    }

    @Override
    public boolean isGravePlayerMade() {
        return false;
    }

    @Override
    public boolean isGravePaymentRequired() {
        return this.getGrave() != null && this.data.isPaymentRequired();
    }

    @Override
    public Text getGravePlaceholder(String id) {
        var x = this.cachedPlaceholders;
        if (x == null) {
            assert this.world != null;
            var server = this.world.getServer();
            x = this.getGrave() != null && server != null ? this.data.getPlaceholders(server) : Map.of();
            this.cachedPlaceholders = x;
        }

        return x.getOrDefault(id, EMPTY_TEXT);
    }

    @Override
    public GameProfile getGraveGameProfile() {
        return this.getGrave() != null ? this.data.getProfile() : Grave.DEFAULT_GAME_PROFILE;
    }

    @Override
    public ItemStack getGraveSlotItem(int i) {
        if (this.getGrave() != null) {
            var items = this.data.getItems();
            if (i < items.size()) {
                return items.get(i).stack();
            }

        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getGraveTaggedItem(Identifier identifier) {
        if (this.getGrave() != null) {
            return this.data.getTaggedItem(identifier);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Arm getGraveMainArm() {
        return this.getGrave() != null ? this.data.mainArm() : Arm.RIGHT;
    }

    @Override
    public byte getGraveSkinModelLayers() {
        return this.getGrave() != null ? this.data.visibleSkinModelParts() : (byte) 0xFF;
    }

    @Override
    public boolean isGravePlayerModelDelayed() {
        return this.getGrave() != null && this.data.delayPlayerModel();
    }
}
