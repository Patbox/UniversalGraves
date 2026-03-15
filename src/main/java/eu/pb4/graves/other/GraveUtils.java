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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class GraveUtils {
    public static final TicketType GRAVE_TICKED = new TicketType(5, 0);

    public static final TagKey<Block> REPLACEABLE_TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("universal_graves", "replaceable"));
    public static final TagKey<Enchantment> BLOCKED_ENCHANTMENTS_TAG = TagKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("universal_graves", "blocked_enchantments"));
    public static final Container EMPTY_INVENTORY = new SimpleContainer(0);
    public static BlockCheckResult findGravePosition(ServerPlayer player, ServerLevel world, BlockPos blockPos, int maxDistance, boolean anyBlock) {
        return findGravePosition(player.getGameProfile(), player, world, blockPos, maxDistance, anyBlock);
    }
    public static BlockCheckResult findGravePosition(GameProfile profile, @Nullable ServerPlayer player, ServerLevel world, BlockPos blockPos, int maxDistance, boolean anyBlock) {
        var border = world.getWorldBorder();
        var config = ConfigManager.getConfig();

        if (config.placement.moveInsideBorder) {
            blockPos = BlockPos.containing(Mth.clamp(blockPos.getX(), border.getMinX() + 1, border.getMaxX() - 1), Mth.clamp(blockPos.getY(), world.getMinY(), world.getMaxY()), Mth.clamp(blockPos.getZ(), border.getMinZ() + 1, border.getMaxZ() - 1));
        } else {
            blockPos = blockPos.atY(Mth.clamp(blockPos.getY(), world.getMinY(), world.getMaxY()));
        }
        if (config.placement.generateOnGround) {
            while (world.getBlockState(blockPos).isAir() && world.getMinY() + 2 < blockPos.getY()) {
                blockPos = blockPos.below();
            }
        }

        var result = isValidPos(profile, player, world, border, blockPos, false, config);
        if (result.allow) {
            return new BlockCheckResult(blockPos, result);
        } else if (result == BlockResult.BLOCK_FLUID) {
            var x = blockPos.mutable();
            while (world.getBlockState(x).getFluidState().getType() != Fluids.EMPTY) {
                x.move(0, 1, 0);
            }
            blockPos = x.immutable();
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

    private static BlockCheckResult findPos(GameProfile profile,  @Nullable ServerPlayer player, ServerLevel world, BlockPos blockPos, int maxDistance, boolean allowAnyBlock, int iteration, Config config) {
        int line = 1;
        var border = world.getWorldBorder();
        BlockResult result = isValidPos(profile, player, world, border, blockPos, allowAnyBlock, config);

        if (result.allow) {
            return new BlockCheckResult(blockPos, result);
        }

        BlockResult tempResult;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());

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
                            return new BlockCheckResult(pos.immutable(), result);
                        }
                    }
                }
            }
            line++;
        }

        if (config.placement.shiftLocationOnFailure && iteration < config.placement.maxShiftCount) {
            return findPos(profile, player, world, blockPos.relative(Direction.getRandom(RandomSource.create()), config.placement.shiftDistance), maxDistance, allowAnyBlock, iteration + 1, config);
        }

        return new BlockCheckResult(null, result);
    }

    public static void spawnExp(ServerLevel world, Vec3 pos, int amount) {
        if (ConfigManager.getConfig().storage.useAlternativeXPEntity) {
            SafeXPEntity.award(world, pos, amount);
        } else {
            ExperienceOrb.award(world, pos, amount);
        }
    }


    private static BlockResult isValidPos(GameProfile profile, @Nullable ServerPlayer player, ServerLevel world, WorldBorder border, BlockPos pos, boolean anyBlock, Config config) {
        BlockState state = world.getBlockState(pos);

        if (canReplaceState(state, anyBlock) && (!config.placement.moveInsideBorder || border.isWithinBounds(pos)) && pos.getY() >= world.getMinY() && pos.getY() < world.getMaxY() + 1) {
            if (config.placement.generateOnTopOfFluids && state.getFluidState().getType() != Fluids.EMPTY && world.getBlockState(pos.above()).getFluidState().getType() != Fluids.EMPTY) {
                return BlockResult.BLOCK_FLUID;
            }

            var areas = config.placement.blacklistedAreas.get(world.dimension().identifier());
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

    public static Component toWorldName(Identifier identifier) {
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
        return Component.literal(String.join(" ", parts));
    }

    public static boolean hasSkippedEnchantment(ItemStack stack) {
        var config = ConfigManager.getConfig();
        for (var enchant : stack.getEnchantments().keySet()) {
            if (enchant.is(BLOCKED_ENCHANTMENTS_TAG)) {
                return true;
            }

            var key = enchant.unwrapKey().get().identifier();
            if (key != null && config.storage.skippedEnchantments.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public static void teleportToGrave(ServerPlayer player, Grave grave, BooleanConsumer finishedCallback) {
        var config = ConfigManager.getConfig();
        var pos = grave.getLocation();
        var movingText = config.teleportation.allowMovingDuringTeleportation || player.isCreative() ? config.teleportation.text.teleportTimerTextAllowMoving : config.teleportation.text.teleportTimerText;

        MinecraftServer server = Objects.requireNonNull(player.level().getServer(), "server; running on client?");
        ServerLevel world = server.getLevel(ResourceKey.create(Registries.DIMENSION, pos.world()));
        if (world != null) {
            player.sendSystemMessage(movingText.with(Map.of("time",
                    Component.nullToEmpty(player.isCreative() ? "0" : Integer.toString(config.teleportation.teleportTime)))));

            GravesMod.DO_ON_NEXT_TICK.add(new Runnable() {
                double x = pos.x();
                double y = pos.y() + config.teleportation.teleportHeight;
                double z = pos.z();

                // If any movement occurs, the teleport request will be cancelled.
                final Vec3 currentPosition = player.position();

                // Non-final to allow for decrementing.
                int teleportTicks = player.isCreative() ? 1 : config.teleportation.teleportTime * 20;
                int invulnerableTicks = config.teleportation.invincibleTime * 20;

                @Override
                public void run() {
                    if (--teleportTicks >= 0) {
                        if (!config.teleportation.allowMovingDuringTeleportation && !player.position().equals(currentPosition)) {
                            player.sendSystemMessage(config.teleportation.text.teleportCancelledText.text());
                            playSoundToPlayer(player, SoundEvents.SHULKER_HURT_CLOSED,
                                    SoundSource.MASTER, 1f, 0.5f);
                            finishedCallback.accept(false);
                            return;
                        }
                        if (teleportTicks == 0) {
                            player.sendSystemMessage(config.teleportation.text.teleportLocationText.with(Map.of("position", Component.translatable("chat.coordinates", x, y, z))));

                            player.teleportTo(world, x + 0.5D, y + 1.0D, z + 0.5D,
                                    Set.of(),
                                    player.getYRot(), player.getXRot(), true);
                            playSoundToPlayer(player, SoundEvents.ENDERMAN_TELEPORT,
                                    SoundSource.MASTER, 1f, 1f);
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

    public static void createGrave(ServerPlayer player, ServerLevel damageWorld, DamageSource source) {
        Config config = ConfigManager.getConfig();


        if (damageWorld.getGameRules().get(GameRules.KEEP_INVENTORY)
                || config.placement.blacklistedWorlds.contains(player.level().dimension().identifier())
                || config.placement.maxGraveCount == 0
        ) {
            return;
        }

        WrappedText text = null;
        var placeholders = Map.of(
                "position", Component.literal(player.blockPosition().toShortString()),
                "world", GraveUtils.toWorldName(player.level().dimension().identifier())
        );

        if (source.getEntity() != null) {
            text = config.placement.ignoredAttackers.get(source.getEntity().getType());
        }


        if (text == null) {
            //noinspection OptionalGetWithoutIsPresent
            text = config.placement.ignoredDamageTypes.get(source.typeHolder().unwrapKey().get().identifier());
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
                var result = GraveUtils.findGravePosition(player, player.level(), player.blockPosition(), config.placement.maxPlacementDistance, config.placement.replaceAnyBlock);

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
                    var world = player.level();

                    var allowedUUID = new HashSet<UUID>();

                    if (config.protection.allowAttackersToTakeItems) {
                        if (source.getEntity() instanceof ServerPlayer playerEntity) {
                            allowedUUID.add(playerEntity.getUUID());
                        }
                        if (player.getLastHurtByMob() instanceof ServerPlayer playerEntity) {
                            allowedUUID.add(playerEntity.getUUID());
                        }
                    }
                    var grave = Grave.createBlock(
                            player,
                            world.dimension().identifier(),
                            gravePos,finalExperience,
                            source.getLocalizedDeathMessage(player),
                            allowedUUID,
                            items,
                            (int) (world.getServer().overworld().getDayTime() / 24000)
                    );

                    ((PlayerAdditions) player).graves$setLastGrave(grave.getId());
                    var oldBlockState = world.getBlockState(gravePos);
                    var fluidState = world.getFluidState(gravePos);
                    world.setBlockAndUpdate(gravePos, GravesRegistry.TEMP_BLOCK.defaultBlockState());

                    world.getChunkSource().addTicketWithRadius(GRAVE_TICKED, ChunkPos.containing(gravePos), 2);

                    GravesMod.DO_ON_NEXT_TICK.add(() -> {
                        WrappedText text2;
                        Map<String, Component> placeholders2 = placeholders;

                        var storedBlockState = world.getBlockState(gravePos).getBlock() == GravesRegistry.TEMP_BLOCK ? oldBlockState : Blocks.AIR.defaultBlockState();

                        world.setBlockAndUpdate(gravePos, GravesRegistry.GRAVE_BLOCK.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, player.getRandom().nextInt(15))
                                .setValue(BlockStateProperties.WATERLOGGED, fluidState.is(Fluids.WATER)));
                        BlockEntity entity = world.getBlockEntity(gravePos);

                        if (entity instanceof GraveBlockEntity graveBlockEntity) {
                            GraveManager.INSTANCE.add(grave);
                            graveBlockEntity.setGrave(grave, storedBlockState);
                            graveBlockEntity.setModelId(model);
                            world.blockEntityChanged(gravePos);
                            text2 = config.texts.messageGraveCreated;
                            placeholders2 = grave.getPlaceholders(player.level().getServer());


                            if (config.placement.maxGraveCount > -1) {
                                var graves = new ArrayList<>(GraveManager.INSTANCE.getByPlayer(player));
                                graves.sort(Comparator.comparing(Grave::getCreationTime));
                                while (graves.size() > config.placement.maxGraveCount) {
                                    graves.removeFirst().destroyGrave(player.level().getServer(), null);
                                }
                            }
                        } else {
                            if (config.storage.xpStorageType != GravesXPCalculation.DROP) {
                                GraveUtils.spawnExp(world, Vec3.atCenterOf(gravePos), finalExperience);
                            }
                            text2 = config.placement.messageCreationFailed;
                            var droppedItems = NonNullList.<ItemStack>createWithCapacity(0);
                            for (var item : items) {
                                droppedItems.add(item.stack());
                            }

                            Containers.dropContents(world, gravePos, droppedItems);
                            ((PlayerAdditions) player).graves$setLastGrave(-1);
                        }
                        if (text2 != null) {
                            player.sendSystemMessage(text2.with(placeholders2));
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
            player.sendSystemMessage(text.with(placeholders));
        }
    }

    public static boolean canReplaceState(BlockState state, boolean dontValidateWithTag) {
        return state.getBlock() != GravesRegistry.TEMP_BLOCK && !state.hasBlockEntity() && (state.isAir() || dontValidateWithTag || state.is(REPLACEABLE_TAG));
    }

    public static void grandExperience(Player player, int experience) {
        player.increaseScore(experience);
        player.experienceProgress += (float)experience / (float)player.getXpNeededForNextLevel();
        player.totalExperience = Mth.clamp(player.totalExperience + experience, 0, 2147483647);

        while(player.experienceProgress < 0.0F) {
            float f = player.experienceProgress * (float)player.getXpNeededForNextLevel();
            if (player.experienceLevel > 0) {
                player.giveExperienceLevels(-1);
                player.experienceProgress = 1.0F + f / (float)player.getXpNeededForNextLevel();
            } else {
                player.giveExperienceLevels(-1);
                player.experienceProgress = 0.0F;
            }
        }

        while(player.experienceProgress >= 1.0F) {
            player.experienceProgress = (player.experienceProgress - 1.0F) * (float)player.getXpNeededForNextLevel();
            player.giveExperienceLevels(1);
            player.experienceProgress /= (float)player.getXpNeededForNextLevel();
        }
    }

    public static void playSoundToPlayer(Player player, SoundEvent soundEvent, SoundSource category, float volume, float pitch) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent), category, player, volume, pitch, player.getRandom().nextLong()));
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
