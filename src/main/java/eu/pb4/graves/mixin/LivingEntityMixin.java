package eu.pb4.graves.mixin;

import eu.pb4.graves.GravesMod;
import eu.pb4.graves.event.PlayerGraveCreationEvent;
import eu.pb4.graves.event.PlayerGraveItemAddedEvent;
import eu.pb4.graves.event.PlayerGraveItemsEvent;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.GraveBlock;
import eu.pb4.graves.grave.GraveBlockEntity;
import eu.pb4.graves.grave.GravesXPCalculation;
import eu.pb4.graves.other.GraveUtils;
import eu.pb4.placeholders.PlaceholderAPI;
import net.fabricmc.fabric.api.tag.TagFactory;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
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

import java.util.*;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private boolean graves_commandKill = false;

    @Inject(method = "kill", at = @At("HEAD"))
    private void graves_onKill(CallbackInfo ci) {
        this.graves_commandKill = true;
    }

    @Inject(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropInventory()V", shift = At.Shift.BEFORE), cancellable = true)
    private void replaceWithGrave(DamageSource source, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayerEntity player) {
            if (player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
                return;
            }

            try {
                Config config = ConfigManager.getConfig();
                Text text = null;
                Map<String, Text> placeholders = Map.of(
                        "position", new LiteralText("" + player.getBlockPos().toShortString()),
                        "world", new LiteralText(GraveUtils.toWorldName(player.getWorld().getRegistryKey().getValue()))
                );


                if (!config.configData.createFromPvP && source.getAttacker() instanceof PlayerEntity) {
                    text = config.creationFailedPvPMessage;
                } else if (!config.configData.createFromCommandDeaths && this.graves_commandKill) {

                } else if (!config.configData.createFromVoid && source == DamageSource.OUT_OF_WORLD && !this.graves_commandKill) {
                    text = config.creationFailedPvPMessage;
                } else {
                    var eventResult = PlayerGraveCreationEvent.EVENT.invoker().shouldCreate(player);

                    if (eventResult.canCreate()) {
                        var result = GraveUtils.findGravePosition(player, player.getWorld(), player.getBlockPos(), config.configData.replaceAnyBlock);

                        if (result.result().canCreate()) {
                            BlockPos gravePos = result.pos();
                            List<ItemStack> items = new ArrayList<>();

                            for (int i = 0; i < player.getInventory().size(); ++i) {
                                ItemStack itemStack = player.getInventory().getStack(i);
                                if (!itemStack.isEmpty()
                                        && PlayerGraveItemAddedEvent.EVENT.invoker().canAddItem(player, itemStack) != ActionResult.FAIL
                                        && !GraveUtils.hasSkippedEnchantment(itemStack)
                                        && !EnchantmentHelper.hasVanishingCurse(itemStack)
                                ) {
                                    items.add(player.getInventory().removeStack(i));
                                }
                            }

                            PlayerGraveItemsEvent.EVENT.invoker().modifyItems(player, items);
                            int i = 0;
                            if (config.xpCalc != GravesXPCalculation.DROP) {
                                i = config.xpCalc.converter.calc(player);
                            }

                            if (items.size() == 0 && i == 0) {
                                return;
                            }

                            if (config.xpCalc != GravesXPCalculation.DROP) {
                                player.experienceLevel = 0;
                            }

                            int finalI = i;
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

                            GravesMod.DO_ON_NEXT_TICK.add(() -> {
                                Text text2 = null;
                                Map<String, Text> placeholders2 = placeholders;
                                BlockState oldBlockState = world.getBlockState(gravePos);
                                world.setBlockState(gravePos, GraveBlock.INSTANCE.getDefaultState().with(Properties.ROTATION, player.getRandom().nextInt(15)));
                                BlockEntity entity = world.getBlockEntity(gravePos);
    
                                if (entity instanceof GraveBlockEntity grave) {
                                    grave.setGrave(gameProfile, items, finalI, source.getDeathMessage(player), oldBlockState, allowedUUID);
                                    text2 = config.createdGraveMessage;
                                    placeholders2 = grave.info.getPlaceholders(player.getServer());
                                } else {
                                    if (config.xpCalc != GravesXPCalculation.DROP) {
                                        ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(gravePos), finalI);
                                    }
                                    text2 = config.creationFailedGraveMessage;
                                    ItemScatterer.spawn(world, gravePos, DefaultedList.copyOf(ItemStack.EMPTY, items.toArray(new ItemStack[0])));
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
