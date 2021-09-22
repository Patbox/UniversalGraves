package eu.pb4.graves.grave;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.other.ImplementedInventory;
import eu.pb4.holograms.api.elements.SpacingHologramElement;
import eu.pb4.holograms.api.holograms.WorldHologram;
import eu.pb4.placeholders.PlaceholderAPI;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GraveBlockEntity extends BlockEntity implements ImplementedInventory, SidedInventory {
    public static BlockEntityType<GraveBlockEntity> BLOCK_ENTITY_TYPE;
    public WorldHologram hologram = null;
    public DefaultedList<ItemStack> stacks = DefaultedList.ofSize(0, ItemStack.EMPTY);
    public GraveInfo info = new GraveInfo();
    public BlockState replacedBlockState = Blocks.AIR.getDefaultState();

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(BLOCK_ENTITY_TYPE, pos, state);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.stacks;
    }

    public void setGrave(GameProfile profile, Collection<ItemStack> itemStacks, int experience, Text deathCause, BlockState blockState) {
        GraveManager.INSTANCE.remove(this.info);

        this.stacks = DefaultedList.ofSize(itemStacks.size() + 5, ItemStack.EMPTY);
        for (ItemStack stack : itemStacks) {
            this.addStack(stack);
        }

        this.replacedBlockState = blockState;
        this.info = new GraveInfo(profile, this.pos, Objects.requireNonNull(this.getWorld()).getRegistryKey().getValue(), System.currentTimeMillis() / 1000, experience, itemStacks.size(), deathCause);
        this.updateItemCount();

        GraveManager.INSTANCE.add(this.info);
        this.markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList nbtList = new NbtList();

        for(int i = 0; i < this.stacks.size(); ++i) {
            ItemStack itemStack = this.stacks.get(i);
            if (!itemStack.isEmpty()) {
                NbtCompound nbtCompound = new NbtCompound();
                itemStack.writeNbt(nbtCompound);
                nbtList.add(nbtCompound);
            }
        }

        if (!nbtList.isEmpty()) {
            nbt.put("Items", nbtList);
        }

        nbt.put("GraveInfo", this.info.writeNbt(new NbtCompound()));
        nbt.put("BlockState", NbtHelper.fromBlockState(this.replacedBlockState));

        return nbt;
    }


    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        try {
            this.info.readNbt(nbt.getCompound("GraveInfo"));

            NbtList nbtList = nbt.getList("Items", 10);
            this.stacks = DefaultedList.ofSize(nbtList.size() + 5, ItemStack.EMPTY);

            for(NbtElement compound : nbtList) {
                this.addStack(ItemStack.fromNbt((NbtCompound) compound));
            }
            this.updateItemCount();
            this.replacedBlockState = NbtHelper.toBlockState((NbtCompound) Objects.requireNonNull(nbt.get("BlockState")));
        } catch (Exception e) {
            // Silence!
        }
    }

    public void onBroken() {
        if (this.hologram != null) {
            this.hologram.hide();
        }
        this.hologram = null;
        Config config = ConfigManager.getConfig();
        Text text = null;

        boolean shouldBreak = this.info.shouldBreak();
        if (!shouldBreak) {
            if (config.graveBrokenMessage != null) {
                text = config.graveBrokenMessage;
            }
        } else {
            if (config.graveExpiredMessage != null) {
                text = config.graveExpiredMessage;
            }
        }

        if (text != null) {
            assert world != null;
            ServerPlayerEntity player = Objects.requireNonNull(world.getServer()).getPlayerManager().getPlayer(this.info.gameProfile.getId());
            if (player != null) {
                player.sendMessage(PlaceholderAPI.parsePredefinedText(text, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, this.info.getPlaceholders()), MessageType.SYSTEM, Util.NIL_UUID);
            }
        }

        GraveManager.INSTANCE.remove(this.info);
        if (config.configData.dropItemsAfterExpiring || !shouldBreak) {
            ItemScatterer.spawn(this.world, this.pos, this);
            ExperienceOrbEntity.spawn((ServerWorld) this.world, Vec3d.ofCenter(this.getPos()), this.info.xp);
        }
    }

    public void updateItemCount() {
        int x = 0;
        for (ItemStack stack : this.getItems()) {
            if (!stack.isEmpty()) {
                x += 1;
            }
        }
        this.info.itemCount = x;

        List<GraveInfo> infoList = GraveManager.INSTANCE.getByUuid(this.info.gameProfile.getId());
        if (infoList != null) {
            int info = infoList.indexOf(this.info);
            if (info != -1) {
                infoList.get(info).itemCount = x;
            }
        }
    }

    @Override
    public void markRemoved() {
        if (this.hologram != null) {
            this.hologram.hide();
        }
        this.hologram = null;
        super.markRemoved();
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof GraveBlockEntity self) || world.isClient() || world.getTime() % 5 != 0) {
            return;
        }

        Config config = ConfigManager.getConfig();

        Map<String, Text> placeholders = self.info.getPlaceholders();

        if (config.configData.breakingTime > -1 && self.info.shouldBreak()) {
            world.setBlockState(pos, self.replacedBlockState, Block.NOTIFY_ALL);
            return;
        }

        boolean isProtected = state.get(GraveBlock.IS_LOCKED);

        if (isProtected && !self.info.isProtected()) {
            world.setBlockState(pos, state.with(GraveBlock.IS_LOCKED, false));
            isProtected = false;


            if (config.noLongerProtectedMessage != null) {
                ServerPlayerEntity player = Objects.requireNonNull(world.getServer()).getPlayerManager().getPlayer(self.info.gameProfile.getId());
                if (player != null) {
                    player.sendMessage(PlaceholderAPI.parsePredefinedText(config.noLongerProtectedMessage, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, placeholders), MessageType.SYSTEM, Util.NIL_UUID);
                }
            }
        }

        if (config.configData.hologram) {
            if (self.hologram == null) {
                self.hologram = new WorldHologram((ServerWorld) world, new Vec3d(pos.getX(), pos.getY(), pos.getZ()).add(0.5, config.configData.hologramOffset, 0.5));
                self.hologram.show();
            }

            List<Text> texts = new ArrayList<>();

            for (Text text : isProtected ? config.hologramProtectedText : config.hologramText) {
                if (text != LiteralText.EMPTY) {
                    texts.add(PlaceholderAPI.parsePredefinedText(text, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, placeholders));
                } else {
                    texts.add(LiteralText.EMPTY);
                }
            }

            int x = 0;

            for (Text text : texts) {
                if (text == LiteralText.EMPTY) {
                    self.hologram.setElement(x, new SpacingHologramElement(0.28));
                } else {
                    self.hologram.setText(x, text);
                }
                x++;
            }
            int size = self.hologram.getElements().size();

            if (x < size) {
                for (; x < size; x++) {
                    self.hologram.removeElement(x);
                }
            }


        } else {
            if (self.hologram != null) {
                self.hologram.hide();
                self.hologram = null;
            }
        }
    }



    @Override
    public int[] getAvailableSlots(Direction side) {
        return IntArrays.EMPTY_ARRAY;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }
}
