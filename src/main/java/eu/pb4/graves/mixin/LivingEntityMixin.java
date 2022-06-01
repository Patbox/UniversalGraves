package eu.pb4.graves.mixin;

import eu.pb4.graves.GravesApi;
import eu.pb4.graves.GravesMod;
import eu.pb4.graves.registry.GraveBlock;
import eu.pb4.graves.registry.GraveBlockEntity;
import eu.pb4.graves.event.PlayerGraveCreationEvent;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.*;
import eu.pb4.graves.other.GraveUtils;
import eu.pb4.graves.other.GravesXPCalculation;
import eu.pb4.graves.other.Location;
import eu.pb4.graves.other.PlayerAdditions;
import eu.pb4.graves.registry.TempBlock;
import eu.pb4.placeholders.PlaceholderAPI;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private boolean graves_commandKill = false;

    @Inject(method = "kill", at = @At("HEAD"))
    private void graves_onKill(CallbackInfo ci) {
        this.graves_commandKill = true;
    }

    @Inject(method = "damage", at = @At("TAIL"))
    private void graves_printDamage1(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (((Object) this) instanceof ServerPlayerEntity player && ((PlayerAdditions) player).graves_getPrintNextDamageSource()) {
            player.sendMessage(new TranslatableText("text.graves.damage_source_info",
                    new LiteralText(source.name).setStyle(Style.EMPTY.withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, source.name)))
            ), false);
        }
    }

    @Inject(method = "applyDamage", at = @At("TAIL"))
    private void graves_printDamage2(DamageSource source, float amount, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayerEntity player && ((PlayerAdditions) player).graves_getPrintNextDamageSource()) {
            player.sendMessage(new TranslatableText("text.graves.damage_source_info",
                    new LiteralText(source.name).setStyle(Style.EMPTY.withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, source.name)))
            ), false);
        }
    }


    @Inject(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropInventory()V", shift = At.Shift.BEFORE), cancellable = true)
    private void replaceWithGrave(DamageSource source, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayerEntity player) {
            try {
                Config config = ConfigManager.getConfig();

                if (player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || config.blacklistedWorlds.contains(player.getWorld().getRegistryKey().getValue())) {
                    return;
                }

                Text text = null;
                Map<String, Text> placeholders = Map.of(
                        "position", new LiteralText("" + player.getBlockPos().toShortString()),
                        "world", GraveUtils.toWorldName(player.getWorld().getRegistryKey().getValue())
                );

                if (!config.configData.createFromPvP && source.getAttacker() instanceof PlayerEntity) {
                    text = config.creationFailedPvPMessage;
                } else if ((!config.configData.createFromCommandDeaths && this.graves_commandKill) || config.configData.blacklistedDamageSources.contains(source.name)) {

                } else if (!config.configData.createFromVoid && source == DamageSource.OUT_OF_WORLD && !this.graves_commandKill) {
                    text = config.creationFailedVoidMessage;
                } else {
                    var eventResult = PlayerGraveCreationEvent.EVENT.invoker().shouldCreate(player);

                    if (eventResult.canCreate()) {
                        var result = GraveUtils.findGravePosition(player, player.getWorld(), player.getBlockPos(), config.configData.maxPlacementDistance, config.configData.replaceAnyBlock);

                        if (result.result().canCreate()) {
                            BlockPos gravePos = result.pos();
                            List<PositionedItemStack> items = new ArrayList<>();

                            for (var mask : GravesApi.getAllInventoryMasks()) {
                                mask.addToGrave(player, (stack, slot, nbt) -> items.add(new PositionedItemStack(stack, slot, mask, nbt)));
                            }

                            int experience = 0;
                            if (config.xpCalc != GravesXPCalculation.DROP) {
                                experience = config.xpCalc.converter.calc(player);
                            }

                            if (items.size() == 0 && experience == 0) {
                                return;
                            }

                            if (config.xpCalc != GravesXPCalculation.DROP) {
                                player.experienceLevel = 0;
                            }

                            int finalExperience = experience;
                            var world = player.getWorld();
                            var gameProfile = player.getGameProfile();

                            var allowedUUID = new HashSet<UUID>();

                            if (config.configData.allowAttackersToTakeItems) {
                                if (source.getAttacker() instanceof ServerPlayerEntity playerEntity) {
                                    allowedUUID.add(playerEntity.getUuid());
                                }
                                if (player.getAttacker() instanceof ServerPlayerEntity playerEntity) {
                                    allowedUUID.add(playerEntity.getUuid());
                                }
                            }
                            var grave = Grave.createBlock(
                                    gameProfile,
                                    world.getRegistryKey().getValue(),
                                    gravePos,finalExperience,
                                    source.getDeathMessage(player),
                                    allowedUUID,
                                    items,
                                    (int) (world.getServer().getOverworld().getTimeOfDay() / 24000)
                            );

                            ((PlayerAdditions) player).graves_setLastGrave(grave.getId());
                            BlockState oldBlockState = world.getBlockState(gravePos);
                            world.setBlockState(gravePos, TempBlock.INSTANCE.getDefaultState());


                            GravesMod.DO_ON_NEXT_TICK.add(() -> {
                                Text text2;
                                Map<String, Text> placeholders2 = placeholders;

                                BlockState storedBlockState = world.getBlockState(gravePos).getBlock() == TempBlock.INSTANCE ? oldBlockState : Blocks.AIR.getDefaultState();

                                world.setBlockState(gravePos, GraveBlock.INSTANCE.getDefaultState().with(Properties.ROTATION, player.getRandom().nextInt(15)));
                                BlockEntity entity = world.getBlockEntity(gravePos);
    
                                if (entity instanceof GraveBlockEntity graveBlockEntity) {
                                    graveBlockEntity.setGrave(grave, storedBlockState);
                                    text2 = config.createdGraveMessage;
                                    placeholders2 = grave.getPlaceholders(player.getServer());
                                } else {
                                    if (config.xpCalc != GravesXPCalculation.DROP) {
                                        GraveUtils.spawnExp(world, Vec3d.ofCenter(gravePos), finalExperience);
                                    }
                                    text2 = config.creationFailedGraveMessage;
                                    var droppedItems = DefaultedList.<ItemStack>ofSize(0);
                                    for (var item : items) {
                                        droppedItems.add(item.stack());
                                    }

                                    ItemScatterer.spawn(world, gravePos, droppedItems);
                                    ((PlayerAdditions) player).graves_setLastGrave(-1);
                                }
                                if (text2 != null) {
                                    player.sendMessage(PlaceholderAPI.parsePredefinedText(text2, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, placeholders2), MessageType.SYSTEM, Util.NIL_UUID);
                                }
                            });
                            
                        } else {
                            text = switch (result.result()) {
                                case BLOCK -> config.creationFailedGraveMessage;
                                case BLOCK_CLAIM -> config.creationFailedClaimMessage;
                                case ALLOW -> null;
                            };
                        }
                    } else {
                        text = switch (eventResult) {
                            case BLOCK -> config.creationFailedGraveMessage;
                            case BLOCK_CLAIM -> config.creationFailedClaimMessage;
                            case BLOCK_PVP -> config.creationFailedPvPMessage;
                            case BLOCK_VOID -> config.creationFailedVoidMessage;
                            default -> null;
                        };
                    }
                }

                if (text != null) {
                    player.sendMessage(PlaceholderAPI.parsePredefinedText(text, PlaceholderAPI.PREDEFINED_PLACEHOLDER_PATTERN, placeholders), MessageType.SYSTEM, Util.NIL_UUID);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
