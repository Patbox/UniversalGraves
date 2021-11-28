package eu.pb4.graves.other;


import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.event.GraveValidPosCheckEvent;
import net.fabricmc.fabric.api.tag.TagFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.border.WorldBorder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GraveUtils {
    public static final Identifier REPLACEABLE_ID = new Identifier("universal_graves", "replaceable");
    public static final Tag<Block> REPLACEABLE_TAG = TagFactory.BLOCK.create(GraveUtils.REPLACEABLE_ID);
    private static final Function<Map.Entry<Property<?>, Comparable<?>>, String> PROPERTY_MAP_PRINTER = new Function<>() {
        public String apply(@Nullable Map.Entry<Property<?>, Comparable<?>> entry) {
            if (entry == null) {
                return "<NULL>";
            } else {
                Property<?> property = entry.getKey();
                return property.getName() + "=" + this.nameValue(property, entry.getValue());
            }
        }

        private <T extends Comparable<T>> String nameValue(Property<T> property, Comparable<?> value) {
            return property.name((T) value);
        }
    };

    public static BlockCheckResult findGravePosition(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos, boolean anyBlock) {
        int maxDistance = 8;
        int line = 1;

        var border = world.getWorldBorder();
        blockPos = new BlockPos(MathHelper.clamp(blockPos.getX(), border.getBoundWest() + 1, border.getBoundEast() - 1), MathHelper.clamp(blockPos.getY(), world.getBottomY(), world.getTopY() - 1), MathHelper.clamp(blockPos.getZ(), border.getBoundNorth() + 1, border.getBoundSouth() - 1));
        BlockResult result = isValidPos(player, world, border, blockPos, anyBlock);
        BlockResult tempResult;
        if (result.allow) {
            return new BlockCheckResult(blockPos, result);
        } else {
            BlockPos.Mutable pos = new BlockPos.Mutable(blockPos.getX(), blockPos.getY(), blockPos.getZ());

            while (line <= maxDistance) {
                int side = line * 2 + 1;
                for (int oY = 0; oY < side; oY++) {
                    for (int oX = 0; oX < side; oX++) {
                        for (int oZ = 0; oZ < side; oZ++) {
                            pos.set(blockPos.getX() - line + oX, blockPos.getY() - line + oY, blockPos.getZ() - line + oZ);

                            if ((oX > 0 && oX < side - 1) && (oY > 0 && oY < side - 1) && (oZ > 0 && oZ < side - 1)) {
                                continue;
                            }

                            tempResult = isValidPos(player, world, border, pos, anyBlock);
                            if (tempResult.priority >= result.priority) {
                                result = tempResult;
                            }
                            if (result.canCreate()) {
                                return new BlockCheckResult(pos.toImmutable(), result);
                            }
                        }
                    }
                }
                line++;
            }
            return new BlockCheckResult(null, result);
        }
    }


    private static BlockResult isValidPos(ServerPlayerEntity player, ServerWorld world, WorldBorder border, BlockPos pos, boolean anyBlock) {
        BlockState state = world.getBlockState(pos);
        if (border.contains(pos) && pos.getY() >= world.getBottomY() && pos.getY() < world.getTopY() && !state.hasBlockEntity() && (state.isAir() || anyBlock || REPLACEABLE_TAG.contains(state.getBlock()))) {
            return GraveValidPosCheckEvent.EVENT.invoker().isValid(player, world, pos);
        } else {
            return BlockResult.BLOCK;
        }
    }

    public static String blockStateToString(BlockState state) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Registry.BLOCK.getId(state.getBlock()));
        if (!state.getEntries().isEmpty()) {
            stringBuilder.append('[');
            stringBuilder.append(state.getEntries().entrySet().stream().map(PROPERTY_MAP_PRINTER).collect(Collectors.joining(",")));
            stringBuilder.append(']');
        }

        return stringBuilder.toString();
    }

    public static String toWorldName(Identifier identifier) {
        var override = ConfigManager.getConfig().configData.worldNameOverrides.get(identifier.toString());
        if (override != null) {
            return override;
        }

        List<String> parts = new ArrayList<>();
        {
            String[] words = identifier.getPath().split("_");
            for (String word : words) {
                String[] s = word.split("", 2);
                s[0] = s[0].toUpperCase(Locale.ROOT);
                parts.add(String.join("", s));
            }
        }
        return String.join(" ", parts);
    }

    public static boolean hasSkippedEnchantment(ItemStack stack) {
        var config = ConfigManager.getConfig().configData;
        for (var enchant : stack.getEnchantments()) {
            if (enchant instanceof NbtCompound compound) {
                var key = EnchantmentHelper.getIdFromNbt(compound);
                if (key != null && config.skippedEnchantments.contains(key.toString()) || (config.tryDetectionSoulbound && (key.getPath().contains("soulbound") || key.getPath().contains("soul_bound")))) {
                    return true;
                }
            }
        }
        return false;
    }


    public enum BlockResult {
        ALLOW(true, 2),
        BLOCK(false, 0),
        BLOCK_CLAIM(false, 1);

        private final boolean allow;
        private final int priority;

        BlockResult(boolean allow, int priority) {
            this.allow = allow;
            this.priority = priority;
        }

        public boolean canCreate() {
            return this.allow;
        }
    }

    public static record BlockCheckResult(@Nullable BlockPos pos, BlockResult result) {
    }
}
