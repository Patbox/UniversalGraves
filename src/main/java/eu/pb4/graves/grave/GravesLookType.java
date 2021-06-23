package eu.pb4.graves.grave;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.ConfigManager;
import net.minecraft.block.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Util;
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
        public NbtCompound getNbtToSend(NbtCompound nbt, int direction, boolean isLocked, GameProfile owner) {
            if (owner != null) {
                NbtCompound nbtCompound = new NbtCompound();
                NbtHelper.writeGameProfile(nbtCompound, owner);
                nbt.put("SkullOwner", nbtCompound);
            }
            return nbt;
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
        public NbtCompound getNbtToSend(NbtCompound nbt, int direction, boolean isLocked, GameProfile owner) {
            NbtCompound skullOwner = new NbtCompound();
            NbtCompound properties = new NbtCompound();
            NbtCompound valueData = new NbtCompound();
            NbtList textures = new NbtList();

            valueData.putString("Value", isLocked ? ConfigManager.getConfig().configData.lockedTexture : ConfigManager.getConfig().configData.unlockedTexture);

            textures.add(valueData);
            properties.put("textures", textures);

            skullOwner.put("Id", NbtHelper.fromUuid(Util.NIL_UUID));
            skullOwner.put("Properties", properties);

            nbt.put("SkullOwner", skullOwner);

            return nbt;
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


    public interface Converter {
        Block getBlock(boolean isLocked);
        BlockState getBlockState(int direction, boolean isLocked);
        NbtCompound getNbtToSend(NbtCompound compound, int direction, boolean isLocked, GameProfile owner);
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
            public NbtCompound getNbtToSend(NbtCompound compound, int direction, boolean isLocked, GameProfile owner) {
                return null;
            }
        };
    }
}
