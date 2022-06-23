package eu.pb4.graves.other;


import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.graves.GravesMod;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.event.GraveValidPosCheckEvent;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.registry.SafeXPEntity;
import eu.pb4.graves.registry.TempBlock;
import eu.pb4.placeholders.api.Placeholders;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Property;
import net.minecraft.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.border.WorldBorder;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static eu.pb4.placeholders.api.Placeholders.PREDEFINED_PLACEHOLDER_PATTERN;

public class GraveUtils {
    public static final Identifier REPLACEABLE_ID = new Identifier("universal_graves", "replaceable");
    public static final TagKey<Block> REPLACEABLE_TAG = TagKey.of(Registry.BLOCK_KEY, REPLACEABLE_ID);
    public static final Inventory EMPTY_INVENTORY = new SimpleInventory(0);
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

        if (config.configData.shiftLocationOnFailure && iteration < config.configData.maxShiftCount) {
            return findPos(player, world, blockPos.offset(Direction.random(Random.create()), config.configData.shiftDistance), maxDistance, allowAnyBlock, iteration + 1, config);
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

            for (var id : CommonProtection.getProviderIds()) {
                if (config.blacklistedProtectedAreas.getBoolean(id) && !CommonProtection.canPlaceBlock(world, pos, player.getGameProfile(), player)) {
                    return BlockResult.BLOCK_CLAIM;
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
        return Text.literal(String.join(" ", parts));
    }

    public static boolean hasSkippedEnchantment(ItemStack stack) {
        var config = ConfigManager.getConfig();
        for (var enchant : stack.getEnchantments()) {
            if (enchant instanceof NbtCompound compound) {
                var key = EnchantmentHelper.getIdFromNbt(compound);
                if (key != null && config.skippedEnchantments.contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void teleportToGrave(ServerPlayerEntity player, Grave grave, BooleanConsumer finishedCallback) {
        var config = ConfigManager.getConfig();
        var pos = grave.getLocation();
        var movingText = config.configData.allowMovingDuringTeleportation ? config.teleportTimerAllowMovingText : config.teleportTimerText;

        MinecraftServer server = Objects.requireNonNull(player.getServer(), "server; running on client?");
        ServerWorld world = server.getWorld(RegistryKey.of(Registry.WORLD_KEY, pos.world()));
        if (world != null) {
            player.sendMessage(Placeholders.parseText(movingText, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, Map.of("time", Text.of(Integer.toString(config.configData.teleportTime)))));

            GravesMod.DO_ON_NEXT_TICK.add(new Runnable() {
                double x = pos.x();
                double y = pos.y() + config.configData.teleportHeight;
                double z = pos.z();

                // If any movement occurs, the teleport request will be cancelled.
                final Vec3d currentPosition = player.getPos();

                // Non-final to allow for decrementing.
                int teleportTicks = config.configData.teleportTime * 20;
                int invulnerableTicks = config.configData.invincibleTime * 20;

                @Override
                public void run() {
                    if (--teleportTicks >= 0) {
                        if (!config.configData.allowMovingDuringTeleportation && !player.getPos().equals(currentPosition)) {
                            player.sendMessage(config.teleportCancelledText);
                            player.playSound(SoundEvents.ENTITY_SHULKER_HURT_CLOSED,
                                    SoundCategory.MASTER, 1f, 0.5f);
                            finishedCallback.accept(false);
                            return;
                        }
                        if (teleportTicks == 0) {
                            player.sendMessage(Placeholders.parseText(config.teleportLocationText, PREDEFINED_PLACEHOLDER_PATTERN, Map.of("position", Text.translatable("chat.coordinates", x, y, z))));

                            player.teleport(world, x + 0.5D, y + 1.0D, z + 0.5D,
                                    player.getYaw(), player.getPitch());
                            player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                                    SoundCategory.MASTER, 1f, 1f);
                            player.setInvulnerable(true);
                        }
                        GravesMod.DO_ON_NEXT_TICK.add(this);
                    } else if (--invulnerableTicks > 0) {
                        GravesMod.DO_ON_NEXT_TICK.add(this);
                    } else {
                        player.setInvulnerable(false);
                        finishedCallback.accept(true);
                    }
                }
            });
        }
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
