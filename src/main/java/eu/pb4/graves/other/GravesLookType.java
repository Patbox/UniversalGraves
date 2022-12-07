package eu.pb4.graves.other;

import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import fr.catcore.server.translations.api.LocalizationTarget;
import fr.catcore.server.translations.api.text.LocalizableText;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.WallShape;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GravesLookType {
    //@formatter:off
    private static final List<GravesLookType> VALUES = new ArrayList<>();
    //@formatter:on

    public static final GravesLookType CHEST = new GravesLookType("chest", false, getChestLike(Blocks.CHEST.getDefaultState(), Blocks.TRAPPED_CHEST.getDefaultState()));
    public static final GravesLookType BARREL = new GravesLookType("barrel", false, getChestLike(Blocks.BARREL.getDefaultState().with(BarrelBlock.OPEN, true), Blocks.BARREL.getDefaultState()));
    public static final GravesLookType PLAYER_HEAD = new GravesLookType("player_head", true, new Converter() {
        @Override
        public Block getBlock(boolean isLocked) {
            return ConfigManager.getConfig().configData.playerHeadTurnIntoSkulls && !isLocked ? Blocks.SKELETON_SKULL : Blocks.PLAYER_HEAD;
        }

        @Override
        public BlockState getBlockState(int direction, boolean isLocked, boolean waterlogged, @Nullable ServerPlayerEntity player) {
            return getBlock(isLocked).getDefaultState().with(PlayerSkullBlock.ROTATION, direction);
        }

        @Override
        public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, VisualGraveData visualData, @Nullable Grave graveInfo, @Nullable Text[] textOverride) {
            if (visualData.gameProfile() != null && (!ConfigManager.getConfig().configData.playerHeadTurnIntoSkulls || isLocked)) {
                var nbt = new NbtCompound();
                NbtCompound nbtCompound = new NbtCompound();
                NbtHelper.writeGameProfile(nbtCompound, visualData.gameProfile());
                nbt.put("SkullOwner", nbtCompound);
                sendHeadToPlayer(player, pos, nbt);
            }
        }
    });
    public static final GravesLookType PRESET_HEAD = new GravesLookType("preset_head", false, new Converter() {
        @Override
        public Block getBlock(boolean isLocked) {
            return Blocks.PLAYER_HEAD;
        }

        @Override
        public BlockState getBlockState(int direction, boolean isLocked, boolean waterlogged, @Nullable ServerPlayerEntity player) {
            return getBlock(isLocked).getDefaultState().with(SkullBlock.ROTATION, direction);
        }

        @Override
        public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, VisualGraveData data, @Nullable Grave grave, @Nullable Text[] textOverride) {
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

    public static final GravesLookType CUSTOM = new GravesLookType("custom", false, new Converter() {
        @Override
        public Block getBlock(boolean isLocked) {
            var config = ConfigManager.getConfig();
            return config.customBlockStateStylesUnlocked[0].state().getBlock();
        }

        @Override
        public BlockState getBlockState(int direction, boolean isLocked, boolean waterlogged, @Nullable ServerPlayerEntity player) {
            var config = ConfigManager.getConfig();
            var list = (isLocked ? config.customBlockStateStylesLocked : config.customBlockStateStylesUnlocked);
            var state = list[direction % list.length].state();
            return state.getBlock() instanceof Waterloggable ? state.with(Properties.WATERLOGGED, waterlogged) : state;
        }

        @Override
        public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, VisualGraveData visualData, @Nullable Grave graveInfo, @Nullable Text[] textOverride) {
            var config = ConfigManager.getConfig();
            var list = (isLocked ? config.customBlockStateStylesLocked : config.customBlockStateStylesUnlocked);
            var entry = list[direction % list.length];

            if (entry.blockEntityType() != null && entry.blockEntityNbt() != null) {
                var compound = entry.blockEntityNbt().copy();

                var texts = isLocked ? config.signProtectedText : config.signText;
                var placeholders = (graveInfo != null ? graveInfo.getPlaceholders(player.getServer()) : visualData.getPlaceholders(player.getServer()));
                var size = Math.min(4, (textOverride != null ? textOverride : texts).length);

                var target = (LocalizationTarget) player;

                for (int i = 0; i < size; i++) {
                    compound.putString("Text" + (i + 1),
                            Text.Serializer.toJson(
                                    textOverride != null ? textOverride[i] : LocalizableText.asLocalizedFor(Placeholders.parseText(texts[i], Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, placeholders), target)
                            )
                    );
                }

                compound.putInt("x", pos.getX());
                compound.putInt("y", pos.getY());
                compound.putInt("z", pos.getZ());

                player.networkHandler.sendPacket(PolymerBlockUtils.createBlockEntityPacket(pos, entry.blockEntityType(), compound));
            }
        }

        @Override
        public int updateRate(BlockState state, BlockPos pos, VisualGraveData visualData, @Nullable Grave graveInfo) {
            return ConfigManager.getConfig().configData.customStyleUpdateRate;
        }
    });

    public static final GravesLookType CLIENT_MODEL = new GravesLookType("client_model", true, new Converter() {
        @Override
        public Block getBlock(boolean isLocked) {
            return isLocked ? Blocks.STONE_BRICK_WALL : Blocks.MOSSY_STONE_BRICK_WALL;
        }

        @Override
        public BlockState getBlockState(int direction, boolean isLocked, boolean waterlogged, @Nullable ServerPlayerEntity player) {
            boolean northSouth = direction > 7;

            return getBlock(isLocked).getDefaultState().with(Properties.WATERLOGGED, waterlogged)
                    .with(WallBlock.UP, false)
                    .with(northSouth ? WallBlock.NORTH_SHAPE : WallBlock.WEST_SHAPE, WallShape.TALL)
                    .with(northSouth ? WallBlock.SOUTH_SHAPE : WallBlock.EAST_SHAPE, WallShape.TALL);
        }
    });

    public static final GravesLookType CLIENT_MODEL_OR_HEAD = new GravesLookType("client_model_or_head", "client_model", true, new Converter() {
        @Override
        public Block getBlock(boolean isLocked) {
            return isLocked ? Blocks.STONE_BRICK_WALL : Blocks.MOSSY_STONE_BRICK_WALL;
        }

        @Override
        public BlockState getBlockState(int direction, boolean isLocked, boolean waterlogged, @Nullable ServerPlayerEntity player) {
            if (player == null || GraveNetworking.canReceive(player.networkHandler)) {
                return GravesLookType.CLIENT_MODEL.converter.getBlockState(direction, isLocked, waterlogged, player);
            } else {
                return GravesLookType.PLAYER_HEAD.converter.getBlockState(direction, isLocked, waterlogged, player);
            }
        }

        @Override
        public void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, VisualGraveData visualData, @Nullable Grave grave, @Nullable Text[] textOverride) {
            if (GraveNetworking.canReceive(player.networkHandler)) {
                GravesLookType.CLIENT_MODEL.converter.sendNbt(player, state, pos, direction, isLocked, visualData, grave, textOverride);
            } else {
                GravesLookType.PLAYER_HEAD.converter.sendNbt(player, state, pos, direction, isLocked, visualData, grave, textOverride);
            }
        }
    });

    public final String name;
    public final Converter converter;
    public final boolean allowClient;
    public final String networkName;

    private GravesLookType(String name, boolean allowClient, Converter converter) {
        this(name, name, allowClient, converter);
    }

    private GravesLookType(String name, String networkName, boolean allowClient, Converter converter) {
        this.name = name;
        this.networkName = networkName;
        this.allowClient = allowClient;
        this.converter = converter;
        VALUES.add(this);
    }

    public static GravesLookType create(Identifier identifier, Converter converter) {
        return new GravesLookType(identifier.toString(), false, converter);
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
            public BlockState getBlockState(int rotation, boolean isLocked, boolean waterlogged, @Nullable ServerPlayerEntity player) {
                boolean chest = this.getBlock(isLocked) instanceof ChestBlock;

                Direction direction = chest ? Direction.fromHorizontal(rotation / 4).getOpposite() : Direction.byId(rotation / 6);

                var state = (isLocked ? locked : unlocked).with(chest ? ChestBlock.FACING : Properties.FACING, direction);
                return state.getBlock() instanceof Waterloggable ? state.with(Properties.WATERLOGGED, waterlogged) : state;
            }
        };
    }

    public static void sendHeadToPlayer(ServerPlayerEntity player, BlockPos pos, NbtCompound compound) {
        if (compound != null) {
            compound.putString("id", "minecraft:skull");
            compound.putInt("x", pos.getX());
            compound.putInt("y", pos.getY());
            compound.putInt("z", pos.getZ());
            player.networkHandler.sendPacket(PolymerBlockUtils.createBlockEntityPacket(pos, BlockEntityType.SKULL, compound));
        }
    }

    public interface Converter {
        Block getBlock(boolean isLocked);

        BlockState getBlockState(int direction, boolean isLocked, boolean waterlogged, @Nullable ServerPlayerEntity player);

        default void sendNbt(ServerPlayerEntity player, BlockState state, BlockPos pos, int direction, boolean isLocked, VisualGraveData visualData, @Nullable Grave grave, @Nullable Text[] textOverride) {
        }

        default int updateRate(BlockState state, BlockPos pos, VisualGraveData visualData, @Nullable Grave graveInfo) {
            return -1;
        }
    }
}
