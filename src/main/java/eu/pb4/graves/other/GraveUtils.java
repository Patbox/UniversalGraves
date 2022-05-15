package eu.pb4.graves.other;


import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.event.GraveValidPosCheckEvent;
import eu.pb4.graves.registry.SafeXPEntity;
import eu.pb4.graves.registry.TempBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagKey;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
    public static final TagKey<Block> REPLACEABLE_TAG = TagKey.of(Registry.BLOCK_KEY, REPLACEABLE_ID);
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

    public static BlockCheckResult findGravePosition(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos, int maxDistance, boolean anyBlock) {
        var border = world.getWorldBorder();
        blockPos = new BlockPos(MathHelper.clamp(blockPos.getX(), border.getBoundWest() + 1, border.getBoundEast() - 1), MathHelper.clamp(blockPos.getY(), world.getBottomY(), world.getTopY() - 1), MathHelper.clamp(blockPos.getZ(), border.getBoundNorth() + 1, border.getBoundSouth() - 1));
        var config = ConfigManager.getConfig();

        var result = isValidPos(player, world, border, blockPos, false, config);
        if (result.allow) {
            return new BlockCheckResult(blockPos, result);
        } else {
            var checkResult = findPos(player, world, blockPos, maxDistance, false, 0, config);

            if (!checkResult.result.allow && anyBlock) {
                checkResult = findPos(player, world, blockPos, maxDistance, true, 0, config);
            }

            return checkResult;
        }
    }

    private static BlockCheckResult findPos(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos, int maxDistance, boolean allowAnyBlock, int iteration, Config config) {
        int line = 1;
        var border = world.getWorldBorder();
        BlockResult result = isValidPos(player, world, border, blockPos, allowAnyBlock, config);

        if (result.allow) {
            return new BlockCheckResult(blockPos, result);
        }

        BlockResult tempResult;
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

                        tempResult = isValidPos(player, world, border, pos, allowAnyBlock, config);
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

    public static void spawnExp(ServerWorld world, Vec3d pos, int amount) {
        if (ConfigManager.getConfig().configData.useAlternativeXPEntity) {
            SafeXPEntity.spawn(world, pos, amount);
        } else {
            ExperienceOrbEntity.spawn(world, pos, amount);
        }
    }


    private static BlockResult isValidPos(ServerPlayerEntity player, ServerWorld world, WorldBorder border, BlockPos pos, boolean anyBlock, Config config) {
        BlockState state = world.getBlockState(pos);

        if (state.getBlock() != TempBlock.INSTANCE && border.contains(pos) && pos.getY() >= world.getBottomY() && pos.getY() < world.getTopY() && !state.hasBlockEntity() && (state.isAir() || anyBlock || state.isIn(REPLACEABLE_TAG))) {
            var areas = config.blacklistedAreas.get(world.getRegistryKey().getValue());
            if (areas != null) {
                for (var area : areas) {
                    if (area.contains(pos.getX(), pos.getY(), pos.getZ())) {
                        return BlockResult.BLOCK_CLAIM;
                    }
                }
            }
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

    public static Text toWorldName(Identifier identifier) {
        var override = ConfigManager.getConfig().worldNameOverrides.get(identifier);
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
        return new LiteralText(String.join(" ", parts));
    }

    public static boolean hasSkippedEnchantment(ItemStack stack) {
        var config = ConfigManager.getConfig();
        for (var enchant : stack.getEnchantments()) {
            if (enchant instanceof NbtCompound compound) {
                var key = EnchantmentHelper.getIdFromNbt(compound);
                if (key != null && config.skippedEnchantments.contains(key) || (config.configData.tryDetectionSoulbound && (key.getPath().contains("soulbound") || key.getPath().contains("soul_bound")))) {
                    return true;
                }
            }
        }
        return false;
    }


    public enum BlockResult {
        ALLOW(true, 3),
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
