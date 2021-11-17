package eu.pb4.graves.grave;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.mixin.BlockEntityUpdateS2CPacketInvoker;
import eu.pb4.placeholders.PlaceholderAPI;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public final class GravesLookType {
    //@formatter:off
    private static final List<GravesLookType> VALUES = new ArrayList<>();
    //@formatter:on

    public static final GravesLookType CHEST = new GravesLookType("chest", getChestLike(Blocks.CHEST.getDefaultState(), Blocks.TRAPPED_CHEST.getDefaultState()));
    public static final GravesLookType BARREL = new GravesLookType("barrel", getChestLike(Blocks.BARREL.getDefaultState().with(BarrelBlock.OPEN, true), Blocks.BARREL.getDefaultState()));
    public static final GravesLookType PLAYER_HEAD = new GravesLookType("player_head", new Converter() {
        @Override
        public Block getBlock(boolean isLocked) {
            return Blocks.PLAYER_HEAD;
        }

        @Override
        public BlockState getBlockState(int direction, boolean isLocked) {
            return getBlock(isLocked).getDefaultState().with(PlayerSkullBlock.ROTATION, direction);
        }

        @Override
        public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, GraveInfo graveInfo) {
            if (graveInfo.gameProfile != null) {
                var nbt = new NbtCompound();
                NbtCompound nbtCompound = new NbtCompound();
                NbtHelper.writeGameProfile(nbtCompound, graveInfo.gameProfile);
                nbt.put("SkullOwner", nbtCompound);
                sendHeadToPlayer(player, pos, nbt);

            }
        }
    });
    public static final GravesLookType PRESET_HEAD = new GravesLookType("preset_head", new Converter() {
        @Override
        public Block getBlock(boolean isLocked) {
            return Blocks.PLAYER_HEAD;
        }

        @Override
        public BlockState getBlockState(int direction, boolean isLocked) {
            return getBlock(isLocked).getDefaultState().with(PlayerSkullBlock.ROTATION, direction);
        }

        @Override
        public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, GraveInfo graveInfo) {
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
    });

    public static final GravesLookType CUSTOM = new GravesLookType("custom", new Converter() {
        @Override
        public Block getBlock(boolean isLocked) {
            var config = ConfigManager.getConfig();
            return config.customBlockStateStylesUnlocked[0].state().getBlock();
        }

        @Override
        public BlockState getBlockState(int direction, boolean isLocked) {
            var config = ConfigManager.getConfig();
            var list = (isLocked ? config.customBlockStateStylesLocked : config.customBlockStateStylesUnlocked);
            return list[direction % list.length].state();
        }

        @Override
        public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, GraveInfo graveInfo) {
            var config = ConfigManager.getConfig();
            var list = (isLocked ? config.customBlockStateStylesLocked : config.customBlockStateStylesUnlocked);
            var entry = list[direction % list.length];

            if (entry.blockEntityType() != null && entry.blockEntityNbt() != null) {
                var compound = entry.blockEntityNbt().copy();

                if (entry.blockEntityType() == BlockEntityType.SIGN) {
                    var texts = isLocked ? config.signProtectedText : config.signText;
                    var placeholders = graveInfo.getPlaceholders();
                    var size = Math.min(4, texts.length);
                    for (int i = 0; i < size; i++) {
                        compound.putString("Text" + (i + 1),
                                Text.Serializer.toJson(
                                        PlaceholderAPI.parsePredefinedText(texts[i], PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, placeholders)
                                )
                        );
                    }
                }

                compound.putInt("x", pos.getX());
                compound.putInt("y", pos.getY());
                compound.putInt("z", pos.getZ());

                player.networkHandler.sendPacket(BlockEntityUpdateS2CPacketInvoker.create(pos, entry.blockEntityType(), compound));
            }
        }

        @Override
        public int updateRate(BlockState state, BlockPos pos, GraveInfo graveInfo) {
            return ConfigManager.getConfig().configData.customStyleUpdateRate;
        }
    });

    public final String name;
    public final Converter converter;

    private GravesLookType(String name, Converter converter) {
        this.name = name;
        this.converter = converter;
        VALUES.add(this);
    }

    public static GravesLookType create(Identifier identifier, Converter converter) {
        return new GravesLookType(identifier.toString(), converter);
    }

    public static GravesLookType byName(String name) {
        for (GravesLookType type : VALUES) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return GravesLookType.PRESET_HEAD;
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
            public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, GraveInfo graveInfo) {
            }
        };
    }

    public static void sendHeadToPlayer(ServerPlayerEntity player, BlockPos pos, NbtCompound compound) {
        if (compound != null) {
            compound.putString("id", "minecraft:skull");
            compound.putInt("x", pos.getX());
            compound.putInt("y", pos.getY());
            compound.putInt("z", pos.getZ());
            player.networkHandler.sendPacket(BlockEntityUpdateS2CPacketInvoker.create(pos, BlockEntityType.SKULL, compound));
        }
    }

    public interface Converter {
        Block getBlock(boolean isLocked);

        BlockState getBlockState(int direction, boolean isLocked);

        void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, GraveInfo graveInfo);

        default int updateRate(BlockState state, BlockPos pos, GraveInfo graveInfo) {
            return -1;
        }
    }
}
