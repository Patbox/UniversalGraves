package eu.pb4.graves.grave;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.ConfigManager;
import net.minecraft.block.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public enum GravesLookType {
    CHEST("chest", getChestLike(Blocks.CHEST.getDefaultState(), Blocks.TRAPPED_CHEST.getDefaultState())),
    BARREL("barrel", getChestLike(Blocks.BARREL.getDefaultState().with(BarrelBlock.OPEN, true), Blocks.BARREL.getDefaultState())),
    PLAYER_HEAD("player_head", new Converter() {
        @Override
        public Block getBlock(boolean isLocked) {
            return Blocks.PLAYER_HEAD;
        }

        @Override
        public BlockState getBlockState(int direction, boolean isLocked) {
            return getBlock(isLocked).getDefaultState().with(PlayerSkullBlock.ROTATION, direction);
        }

        @Override
        public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, GameProfile owner) {
            if (owner != null) {
                var nbt = new NbtCompound();
                NbtCompound nbtCompound = new NbtCompound();
                NbtHelper.writeGameProfile(nbtCompound, owner);
                nbt.put("SkullOwner", nbtCompound);
                sendHeadToPlayer(player, pos, nbt);

            }
        }
    }),
    PRESET_HEAD("preset_head", new Converter() {
        @Override
        public Block getBlock(boolean isLocked) {
            return Blocks.PLAYER_HEAD;
        }

        @Override
        public BlockState getBlockState(int direction, boolean isLocked) {
            return getBlock(isLocked).getDefaultState().with(PlayerSkullBlock.ROTATION, direction);
        }

        @Override
        public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, GameProfile owner) {
            var nbt = new NbtCompound();
            NbtCompound skullOwner = new NbtCompound();
            NbtCompound properties = new NbtCompound();
            NbtCompound valueData = new NbtCompound();
            NbtList textures = new NbtList();

            valueData.putString("Value", isLocked ? ConfigManager.getConfig().configData.presetHeadLockedTexture : ConfigManager.getConfig().configData.presetHeadUnlockedTexture);

            textures.add(valueData);
            properties.put("textures", textures);

            skullOwner.put("Id", NbtHelper.fromUuid(Util.NIL_UUID));
            skullOwner.put("Properties", properties);

            nbt.put("SkullOwner", skullOwner);

            sendHeadToPlayer(player, pos, nbt);
        }
    }),
    CUSTOM("custom", new Converter() {
        @Override
        public Block getBlock(boolean isLocked) {
            var config = ConfigManager.getConfig();
            return config.customBlockStateStylesUnlocked.get(0).state().getBlock();
        }

        @Override
        public BlockState getBlockState(int direction, boolean isLocked) {
            var config = ConfigManager.getConfig();
            var list = (isLocked ? config.customBlockStateStylesLocked : config.customBlockStateStylesUnlocked);
            return list.get(direction % list.size()).state();
        }

        @Override
        public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, GameProfile owner) {
            var config = ConfigManager.getConfig();
            var list = (isLocked ? config.customBlockStateStylesLocked : config.customBlockStateStylesUnlocked);
            var entry = list.get(direction % list.size());

            if (entry.blockEntityId() != -1 && entry.blockEntityNbt() != null) {
                var compound = entry.blockEntityNbt().copy();

                compound.putInt("x", pos.getX());
                compound.putInt("y", pos.getY());
                compound.putInt("z", pos.getZ());

                player.networkHandler.sendPacket(new BlockEntityUpdateS2CPacket(pos, entry.blockEntityId(), compound));
            }
        }
    });

    public final String name;
    public final Converter converter;

    GravesLookType(String name, Converter converter) {
        this.name = name;
        this.converter = converter;
    }

    public static GravesLookType byName(String name) {
        for (GravesLookType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return GravesLookType.PRESET_HEAD;
    }

    public static String next(String lookType) {
        var byName = byName(lookType);

        return GravesLookType.values()[(byName.ordinal() + 1) % GravesLookType.values().length].name;
    }

    public interface Converter {
        Block getBlock(boolean isLocked);
        BlockState getBlockState(int direction, boolean isLocked);
        void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, GameProfile owner);
    }

    private static Converter getChestLike(BlockState unlocked, BlockState locked) {
        return new Converter() {
            @Override
            public Block getBlock(boolean isLocked) {
                return isLocked ? locked.getBlock() : unlocked.getBlock();
            }

            @Override
            public BlockState getBlockState(int rotation, boolean isLocked) {
                boolean chest = this.getBlock(isLocked) instanceof ChestBlock;

                Direction direction = chest ? Direction.fromHorizontal(rotation / 4).getOpposite() : Direction.byId(rotation / 6);

                return (isLocked ? locked : unlocked).with(chest ? ChestBlock.FACING : Properties.FACING, direction);
            }

            @Override
            public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, GameProfile owner) { }
        };
    }

    public static void sendHeadToPlayer(ServerPlayerEntity player, BlockPos pos, NbtCompound compound) {
        if (compound != null) {
            compound.putString("id", "minecraft:skull");
            compound.putInt("x", pos.getX());
            compound.putInt("y", pos.getY());
            compound.putInt("z", pos.getZ());
            player.networkHandler.sendPacket(new BlockEntityUpdateS2CPacket(pos, 4, compound));
        }
    }
}
