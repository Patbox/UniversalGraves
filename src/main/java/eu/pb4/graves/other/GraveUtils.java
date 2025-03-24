package eu.pb4.graves.other;


import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.graves.GravesApi;
import eu.pb4.graves.GravesMod;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.config.data.WrappedText;
import eu.pb4.graves.event.GraveValidPosCheckEvent;
import eu.pb4.graves.event.PlayerGraveCreationEvent;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.grave.PositionedItemStack;
import eu.pb4.graves.registry.*;
import eu.pb4.predicate.api.PredicateContext;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.border.WorldBorder;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class GraveUtils {
    public static final ChunkTicketType GRAVE_TICKED = new ChunkTicketType(5, false, ChunkTicketType.Use.LOADING);

    public static final TagKey<Block> REPLACEABLE_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of("universal_graves", "replaceable"));
    public static final TagKey<Enchantment> BLOCKED_ENCHANTMENTS_TAG = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("universal_graves", "blocked_enchantments"));
    public static final Inventory EMPTY_INVENTORY = new SimpleInventory(0);
    public static BlockCheckResult findGravePosition(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos, int maxDistance, boolean anyBlock) {
        return findGravePosition(player.getGameProfile(), player, world, blockPos, maxDistance, anyBlock);
    }
    public static BlockCheckResult findGravePosition(GameProfile profile, @Nullable ServerPlayerEntity player, ServerWorld world, BlockPos blockPos, int maxDistance, boolean anyBlock) {
        var border = world.getWorldBorder();
        var config = ConfigManager.getConfig();

        if (config.placement.moveInsideBorder) {
            blockPos = BlockPos.ofFloored(MathHelper.clamp(blockPos.getX(), border.getBoundWest() + 1, border.getBoundEast() - 1), MathHelper.clamp(blockPos.getY(), world.getBottomY(), world.getTopYInclusive()), MathHelper.clamp(blockPos.getZ(), border.getBoundNorth() + 1, border.getBoundSouth() - 1));
        } else {
            blockPos = blockPos.withY(MathHelper.clamp(blockPos.getY(), world.getBottomY(), world.getTopYInclusive()));
        }
        if (config.placement.generateOnGround) {
            while (world.getBlockState(blockPos).isAir() && world.getBottomY() + 2 < blockPos.getY()) {
                blockPos = blockPos.down();
            }
        }

        var result = isValidPos(profile, player, world, border, blockPos, false, config);
        if (result.allow) {
            return new BlockCheckResult(blockPos, result);
        } else if (result == BlockResult.BLOCK_FLUID) {
            var x = blockPos.mutableCopy();
            while (world.getBlockState(x).getFluidState().getFluid() != Fluids.EMPTY) {
                x.move(0, 1, 0);
            }
            blockPos = x.toImmutable();
            result = isValidPos(profile, player, world, border, blockPos, false, config);
            if (result.allow) {
                return new BlockCheckResult(blockPos, result);
            }
        }

        var checkResult = findPos(profile, player, world, blockPos, maxDistance, false, 0, config);

        if (!checkResult.result.allow && anyBlock) {
            checkResult = findPos(profile, player, world, blockPos, maxDistance, true, 0, config);
        }

        return checkResult;
    }

    private static BlockCheckResult findPos(GameProfile profile,  @Nullable ServerPlayerEntity player, ServerWorld world, BlockPos blockPos, int maxDistance, boolean allowAnyBlock, int iteration, Config config) {
        int line = 1;
        var border = world.getWorldBorder();
        BlockResult result = isValidPos(profile, player, world, border, blockPos, allowAnyBlock, config);

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

                        tempResult = isValidPos(profile, player, world, border, pos, allowAnyBlock, config);
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

        if (config.placement.shiftLocationOnFailure && iteration < config.placement.maxShiftCount) {
            return findPos(profile, player, world, blockPos.offset(Direction.random(Random.create()), config.placement.shiftDistance), maxDistance, allowAnyBlock, iteration + 1, config);
        }

        return new BlockCheckResult(null, result);
    }

    public static void spawnExp(ServerWorld world, Vec3d pos, int amount) {
        if (ConfigManager.getConfig().storage.useAlternativeXPEntity) {
            SafeXPEntity.spawn(world, pos, amount);
        } else {
            ExperienceOrbEntity.spawn(world, pos, amount);
        }
    }


    private static BlockResult isValidPos(GameProfile profile, @Nullable ServerPlayerEntity player, ServerWorld world, WorldBorder border, BlockPos pos, boolean anyBlock, Config config) {
        BlockState state = world.getBlockState(pos);

        if (canReplaceState(state, anyBlock) && (!config.placement.moveInsideBorder || border.contains(pos)) && pos.getY() >= world.getBottomY() && pos.getY() < world.getTopYInclusive() + 1) {
            if (config.placement.generateOnTopOfFluids && state.getFluidState().getFluid() != Fluids.EMPTY && world.getBlockState(pos.up()).getFluidState().getFluid() != Fluids.EMPTY) {
                return BlockResult.BLOCK_FLUID;
            }

            var areas = config.placement.blacklistedAreas.get(world.getRegistryKey().getValue());
            if (areas != null) {
                for (var area : areas) {
                    if (area.contains(pos.getX(), pos.getY(), pos.getZ())) {
                        return BlockResult.BLOCK_CLAIM;
                    }
                }
            }

            //noinspection ConstantConditions
            for (var id : CommonProtection.getProviderIds()) {
                if (config.placement.blockInProtection.get(id) == Boolean.TRUE && !Objects.requireNonNull(CommonProtection.getProvider(id)).canPlaceBlock(world, pos, profile, player)) {
                    return BlockResult.BLOCK_CLAIM;
                }
            }

            return GraveValidPosCheckEvent.EVENT.invoker().isValid(profile, world, pos);
        } else {
            return BlockResult.BLOCK;
        }
    }

    public static Text toWorldName(Identifier identifier) {
        var override = ConfigManager.getConfig().texts.worldNameOverrides.get(identifier);
        if (override != null) {
            return override.text();
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
        for (var enchant : stack.getEnchantments().getEnchantments()) {
            if (enchant.isIn(BLOCKED_ENCHANTMENTS_TAG)) {
                return true;
            }

            var key = enchant.getKey().get().getValue();
            if (key != null && config.storage.skippedEnchantments.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public static void teleportToGrave(ServerPlayerEntity player, Grave grave, BooleanConsumer finishedCallback) {
        var config = ConfigManager.getConfig();
        var pos = grave.getLocation();
        var movingText = config.teleportation.allowMovingDuringTeleportation || player.isCreative() ? config.teleportation.text.teleportTimerTextAllowMoving : config.teleportation.text.teleportTimerText;

        MinecraftServer server = Objects.requireNonNull(player.getServer(), "server; running on client?");
        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, pos.world()));
        if (world != null) {
            player.sendMessage(movingText.with(Map.of("time",
                    Text.of(player.isCreative() ? "0" : Integer.toString(config.teleportation.teleportTime)))));

            GravesMod.DO_ON_NEXT_TICK.add(new Runnable() {
                double x = pos.x();
                double y = pos.y() + config.teleportation.teleportHeight;
                double z = pos.z();

                // If any movement occurs, the teleport request will be cancelled.
                final Vec3d currentPosition = player.getPos();

                // Non-final to allow for decrementing.
                int teleportTicks = player.isCreative() ? 1 : config.teleportation.teleportTime * 20;
                int invulnerableTicks = config.teleportation.invincibleTime * 20;

                @Override
                public void run() {
                    if (--teleportTicks >= 0) {
                        if (!config.teleportation.allowMovingDuringTeleportation && !player.getPos().equals(currentPosition)) {
                            player.sendMessage(config.teleportation.text.teleportCancelledText.text());
                            player.playSoundToPlayer(SoundEvents.ENTITY_SHULKER_HURT_CLOSED,
                                    SoundCategory.MASTER, 1f, 0.5f);
                            finishedCallback.accept(false);
                            return;
                        }
                        if (teleportTicks == 0) {
                            player.sendMessage(config.teleportation.text.teleportLocationText.with(Map.of("position", Text.translatable("chat.coordinates", x, y, z))));

                            player.teleport(world, x + 0.5D, y + 1.0D, z + 0.5D,
                                    Set.of(),
                                    player.getYaw(), player.getPitch(), true);
                            player.playSoundToPlayer(SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                                    SoundCategory.MASTER, 1f, 1f);
                            ((PlayerAdditions) player).graves$setInvulnerable(true);
                        }
                        GravesMod.DO_ON_NEXT_TICK.add(this);
                    } else if (--invulnerableTicks > 0) {
                        GravesMod.DO_ON_NEXT_TICK.add(this);
                    } else {
                        ((PlayerAdditions) player).graves$setInvulnerable(false);
                        finishedCallback.accept(true);
                    }
                }
            });
        }
    }

    public static void createGrave(ServerPlayerEntity player, ServerWorld damageWorld, DamageSource source) {
        Config config = ConfigManager.getConfig();


        if (damageWorld.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)
                || config.placement.blacklistedWorlds.contains(player.getWorld().getRegistryKey().getValue())
                || config.placement.maxGraveCount == 0
        ) {
            return;
        }

        WrappedText text = null;
        var placeholders = Map.of(
                "position", Text.literal(player.getBlockPos().toShortString()),
                "world", GraveUtils.toWorldName(player.getWorld().getRegistryKey().getValue())
        );

        if (source.getAttacker() != null) {
            text = config.placement.ignoredAttackers.get(source.getAttacker().getType());
        }


        if (text == null) {
            //noinspection OptionalGetWithoutIsPresent
            text = config.placement.ignoredDamageTypes.get(source.getTypeRegistryEntry().getKey().get().getValue());
        }

        if (text == null) {
            var ctx = PredicateContext.of(player);
            for (var test : config.placement.predicates) {
                if (test.predicate.test(ctx).success()) {
                    text = test.text;
                }
            }
        }

        if (text == null) {
            var eventResult = PlayerGraveCreationEvent.EVENT.invoker().shouldCreate(player);

            if (eventResult.canCreate()) {
                var result = GraveUtils.findGravePosition(player, player.getServerWorld(), player.getBlockPos(), config.placement.maxPlacementDistance, config.placement.replaceAnyBlock);

                if (result.result().canCreate()) {
                    var model = config.getGraveModel(player);

                    BlockPos gravePos = result.pos();
                    if (gravePos == null) {
                        return;
                    }
                    List<PositionedItemStack> items = new ArrayList<>();

                    for (var mask : GravesApi.getAllInventoryMasks()) {
                        try {
                            mask.addToGrave(player, (stack, slot, nbt, tags) -> items.add(new PositionedItemStack(stack, slot, mask, nbt, Set.of(tags))));
                        } catch (Throwable e) {
                            GravesMod.LOGGER.error("Failed to add items from '" + mask.getId() + "'!", e);
                        }
                    }

                    int experience = 0;
                    if (config.storage.xpStorageType != GravesXPCalculation.DROP) {
                        experience = config.storage.xpStorageType.converter.calc(player);
                    }

                    if (items.isEmpty() && (!config.storage.canStoreOnlyXp || experience == 0)) {
                        return;
                    }

                    if (config.storage.xpStorageType != GravesXPCalculation.DROP) {
                        player.experienceLevel = 0;
                    }

                    int finalExperience = experience;
                    var world = player.getServerWorld();

                    var allowedUUID = new HashSet<UUID>();

                    if (config.protection.allowAttackersToTakeItems) {
                        if (source.getAttacker() instanceof ServerPlayerEntity playerEntity) {
                            allowedUUID.add(playerEntity.getUuid());
                        }
                        if (player.getAttacker() instanceof ServerPlayerEntity playerEntity) {
                            allowedUUID.add(playerEntity.getUuid());
                        }
                    }
                    var grave = Grave.createBlock(
                            player,
                            world.getRegistryKey().getValue(),
                            gravePos,finalExperience,
                            source.getDeathMessage(player),
                            allowedUUID,
                            items,
                            (int) (world.getServer().getOverworld().getTimeOfDay() / 24000)
                    );

                    ((PlayerAdditions) player).graves$setLastGrave(grave.getId());
                    var oldBlockState = world.getBlockState(gravePos);
                    var fluidState = world.getFluidState(gravePos);
                    world.setBlockState(gravePos, GravesRegistry.TEMP_BLOCK.getDefaultState());

                    world.getChunkManager().addTicket(GRAVE_TICKED, new ChunkPos(gravePos), 2);

                    GravesMod.DO_ON_NEXT_TICK.add(() -> {
                        WrappedText text2;
                        Map<String, Text> placeholders2 = placeholders;

                        var storedBlockState = world.getBlockState(gravePos).getBlock() == GravesRegistry.TEMP_BLOCK ? oldBlockState : Blocks.AIR.getDefaultState();

                        world.setBlockState(gravePos, GravesRegistry.GRAVE_BLOCK.getDefaultState().with(Properties.ROTATION, player.getRandom().nextInt(15))
                                .with(Properties.WATERLOGGED, fluidState.isOf(Fluids.WATER)));
                        BlockEntity entity = world.getBlockEntity(gravePos);

                        if (entity instanceof GraveBlockEntity graveBlockEntity) {
                            GraveManager.INSTANCE.add(grave);
                            graveBlockEntity.setGrave(grave, storedBlockState);
                            graveBlockEntity.setModelId(model);
                            world.markDirty(gravePos);
                            text2 = config.texts.messageGraveCreated;
                            placeholders2 = grave.getPlaceholders(player.getServer());


                            if (config.placement.maxGraveCount > -1) {
                                var graves = new ArrayList<>(GraveManager.INSTANCE.getByPlayer(player));
                                graves.sort(Comparator.comparing(Grave::getCreationTime));
                                while (graves.size() > config.placement.maxGraveCount) {
                                    graves.remove(0).destroyGrave(player.server, null);
                                }
                            }
                        } else {
                            if (config.storage.xpStorageType != GravesXPCalculation.DROP) {
                                GraveUtils.spawnExp(world, Vec3d.ofCenter(gravePos), finalExperience);
                            }
                            text2 = config.placement.messageCreationFailed;
                            var droppedItems = DefaultedList.<ItemStack>ofSize(0);
                            for (var item : items) {
                                droppedItems.add(item.stack());
                            }

                            ItemScatterer.spawn(world, gravePos, droppedItems);
                            ((PlayerAdditions) player).graves$setLastGrave(-1);
                        }
                        if (text2 != null) {
                            player.sendMessage(text2.with(placeholders2));
                        }
                    });

                } else {
                    text = switch (result.result()) {
                        case BLOCK -> config.placement.messageCreationFailed;
                        case BLOCK_CLAIM -> config.placement.messageCreationFailedClaim;
                        default -> null;
                    };
                }
            } else {
                text = switch (eventResult) {
                    case BLOCK -> config.placement.messageCreationFailed;
                    case BLOCK_CLAIM -> config.placement.messageCreationFailedClaim;
                    default -> null;
                };
            }
        }

        if (text != null && !text.isEmpty()) {
            player.sendMessage(text.with(placeholders));
        }
    }

    public static boolean canReplaceState(BlockState state, boolean dontValidateWithTag) {
        return state.getBlock() != GravesRegistry.TEMP_BLOCK && !state.hasBlockEntity() && (state.isAir() || dontValidateWithTag || state.isIn(REPLACEABLE_TAG));
    }

    public static void grandExperience(PlayerEntity player, int experience) {
        player.addScore(experience);
        player.experienceProgress += (float)experience / (float)player.getNextLevelExperience();
        player.totalExperience = MathHelper.clamp(player.totalExperience + experience, 0, 2147483647);

        while(player.experienceProgress < 0.0F) {
            float f = player.experienceProgress * (float)player.getNextLevelExperience();
            if (player.experienceLevel > 0) {
                player.addExperienceLevels(-1);
                player.experienceProgress = 1.0F + f / (float)player.getNextLevelExperience();
            } else {
                player.addExperienceLevels(-1);
                player.experienceProgress = 0.0F;
            }
        }

        while(player.experienceProgress >= 1.0F) {
            player.experienceProgress = (player.experienceProgress - 1.0F) * (float)player.getNextLevelExperience();
            player.addExperienceLevels(1);
            player.experienceProgress /= (float)player.getNextLevelExperience();
        }
    }


    public enum BlockResult {
        ALLOW(true, 999),
        BLOCK(false, 0),
        BLOCK_CLAIM(false, 1),
        BLOCK_FLUID(false, 2);

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
