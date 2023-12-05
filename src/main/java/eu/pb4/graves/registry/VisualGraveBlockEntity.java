package eu.pb4.graves.registry;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.model.GraveModelHandler;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SignGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.IntFunction;

import static eu.pb4.graves.registry.AbstractGraveBlock.IS_LOCKED;

public class VisualGraveBlockEntity extends AbstractGraveBlockEntity {
    public static BlockEntityType<VisualGraveBlockEntity> BLOCK_ENTITY_TYPE;
    public BlockState replacedBlockState = Blocks.AIR.getDefaultState();
    private VisualGraveData visualData = VisualGraveData.DEFAULT;
    protected boolean isPlayerMade = false;
    protected Text[] textOverrides = null;
    private GraveModelHandler model;
    private Map<String, Text> cachedPlaceholders;

    public VisualGraveBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY_TYPE, pos, state);
    }

    public VisualGraveBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
    }

    public void setVisualData(VisualGraveData data, BlockState oldBlockState) {
        this.replacedBlockState = oldBlockState;
        this.visualData = data;
        this.cachedPlaceholders = null;
        if (this.model != null) {
            this.model.setGrave(this);
        }

        this.markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put("BlockState", NbtHelper.fromBlockState(this.replacedBlockState));
        nbt.put("VisualData", this.visualData.toNbt());
        nbt.putBoolean("AllowModification", this.isPlayerMade);

        if (this.textOverrides != null) {
            var list = new NbtList();

            for (var text : this.textOverrides) {
                list.add(NbtString.of(Text.Serialization.toJsonString(text)));
            }

            nbt.put("TextOverride", list);
        }
    }


    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        try {
            this.visualData = VisualGraveData.fromNbt(nbt.getCompound("VisualData"));
            this.replacedBlockState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), (NbtCompound) Objects.requireNonNull(nbt.get("BlockState")));

            if (nbt.contains("TextOverride", NbtElement.LIST_TYPE)) {
                var textOverrides = new ArrayList<>();
                for (var text : nbt.getList("TextOverride", NbtElement.STRING_TYPE)) {
                    textOverrides.add(Text.Serialization.fromLenientJson(text.asString()));
                }
                this.textOverrides = textOverrides.toArray(new Text[0]);
            }
        } catch (Exception e) {
            if (this.visualData == null) {
                this.visualData = VisualGraveData.DEFAULT;
            }
        }
        this.cachedPlaceholders = null;
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof VisualGraveBlockEntity self) || world.isClient()) {
            return;
        }

        if (self.model == null) {
            self.model = (GraveModelHandler) BlockBoundAttachment.get(world, pos).holder();
            self.model.setGrave(self);
        }

        self.model.maybeTick(world.getTime());
    }

    protected Map<String, Text> createPlaceholders() {
        var placeholder = this.getGrave().getPlaceholders(this.world.getServer());

        if (this.textOverrides != null) {
            placeholder.put("text_1", this.textOverrides[0]);
            placeholder.put("text_2", this.textOverrides[1]);
            placeholder.put("text_3", this.textOverrides[2]);
            placeholder.put("text_4", this.textOverrides[3]);
        } else {
            placeholder.put("text_1", Text.empty());
            placeholder.put("text_2", Text.empty());
            placeholder.put("text_3", Text.empty());
            placeholder.put("text_4", Text.empty());
        }
        return placeholder;
    }

    public VisualGraveData getGrave() {
        return this.visualData;
    }

    @Override
    public VisualGraveData getClientData() {
        return this.visualData;
    }

    @Override
    public void onModelChanged(String model) {
        if (this.model != null) {
            this.model.updateModel();
        }
    }

    public void openEditScreen(ServerPlayerEntity player) {
        var sign = new SignGui(player) {
            @Override
            public void onClose() {
                VisualGraveBlockEntity.this.textOverrides = new Text[]{
                        this.getLine(0),
                        this.getLine(1),
                        this.getLine(2),
                        this.getLine(3)
                };
                VisualGraveBlockEntity.this.cachedPlaceholders = null;
                VisualGraveBlockEntity.this.markDirty();
            }
        };
        sign.setSignType(Blocks.BIRCH_SIGN);

        if (this.textOverrides != null) {
            int i = 0;

            for (var text : this.textOverrides) {
                sign.setLine(i, text.copy());
                i++;

                if (i == 4) {
                    break;
                }
            }
        }

        sign.open();
    }

    @Override
    public boolean isGraveProtected() {
        return this.getCachedState().get(IS_LOCKED);
    }

    @Override
    public boolean isGraveBroken() {
        return true;
    }

    @Override
    public boolean isGravePlayerMade() {
        return this.isPlayerMade;
    }

    @Override
    public boolean isGravePaymentRequired() {
        return false;
    }

    @Override
    public Text getGravePlaceholder(String id) {
        var x = this.cachedPlaceholders;
        if (x == null) {
            x = this.createPlaceholders();
            this.cachedPlaceholders = x;
        }

        return x.getOrDefault(id, EMPTY_TEXT);
    }

    @Override
    public GameProfile getGraveGameProfile() {
        return this.getGrave().gameProfile();
    }

    @Override
    public ItemStack getGraveSlotItem(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getGraveTaggedItem(Identifier identifier) {
        return ItemStack.EMPTY;
    }

    @Override
    public Arm getGraveMainArm() {
        return this.getGrave().mainArm();
    }

    @Override
    public byte getGraveSkinModelLayers() {
        return this.getGrave().visualSkinModelLayers();
    }

    @Override
    public boolean isGravePlayerModelDelayed() {
        return false;
    }

    @Override
    public void updateModel() {
        if (this.model != null) {
            this.model.updateModel();
        }
    }
}
