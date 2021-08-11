package eu.pb4.graves.mixin;

import eu.pb4.graves.event.PlayerGraveCreationEvent;
import eu.pb4.graves.event.PlayerGraveItemAddedEvent;
import eu.pb4.graves.event.PlayerGraveItemsEvent;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.GraveBlock;
import eu.pb4.graves.grave.GraveBlockEntity;
import eu.pb4.graves.other.GraveUtils;
import eu.pb4.placeholders.PlaceholderAPI;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
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
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow protected int playerHitTimer;

    @Inject(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropInventory()V", shift = At.Shift.BEFORE), cancellable = true)
    private void replaceWithGrave(DamageSource source, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayerEntity player) {
            if (player.getServerWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
                return;
            }

            try {
                Config config = ConfigManager.getConfig();
                Text text;
                Map<String, Text> placeholders = Map.of(
                        "position", new LiteralText("" + player.getBlockPos().toShortString()),
                        "world", new LiteralText(GraveUtils.toWorldName(player.getServerWorld().getRegistryKey().getValue()))
                );


                if (!config.configData.createGravesFromPvP && source.getAttacker() instanceof PlayerEntity) {
                    text = config.creationFailedPvPGraveMessage;
                } else {
                    var eventResult = PlayerGraveCreationEvent.EVENT.invoker().shouldCreate(player);

                    if (eventResult.canCreate()) {
                        var result = GraveUtils.findGravePosition(player, player.getServerWorld(), player.getBlockPos(), TagRegistry.block(GraveUtils.REPLACEABLE_TAG));

                        if (result.result().canCreate()) {
                            BlockPos gravePos = result.pos();
                            List<ItemStack> items = new ArrayList<>();

                            for (int i = 0; i < player.getInventory().size(); ++i) {
                                ItemStack itemStack = player.getInventory().getStack(i);
                                if (!itemStack.isEmpty()
                                        && PlayerGraveItemAddedEvent.EVENT.invoker().canAddItem(player, itemStack) != ActionResult.FAIL
                                        && !GraveUtils.hasSoulboundEnchantment(itemStack)) {
                                    items.add(player.getInventory().removeStack(i));
                                }
                            }

                            PlayerGraveItemsEvent.EVENT.invoker().modifyItems(player, items);
                            int i = 0;
                            if (config.configData.storeExperience) {
                                i = config.xpCalc.converter.calc(player);
                            }

                            if (items.size() == 0 && i == 0) {
                                return;
                            }

                            BlockState oldBlockState = player.getServerWorld().getBlockState(gravePos);
                            player.getServerWorld().setBlockState(gravePos, GraveBlock.INSTANCE.getDefaultState().with(Properties.ROTATION, player.getRandom().nextInt(15)));
                            BlockEntity entity = player.getServerWorld().getBlockEntity(gravePos);

                            if (entity instanceof GraveBlockEntity grave) {
                                if (config.configData.storeExperience) {
                                    player.experienceLevel = 0;
                                }

                                grave.setGrave(player.getGameProfile(), items, i, source.getDeathMessage(player), oldBlockState);
                                text = config.createdGraveMessage;
                                placeholders = grave.info.getPlaceholders();
                            } else {
                                text = config.creationFailedGraveMessage;
                                ItemScatterer.spawn(player.getServerWorld(), gravePos, DefaultedList.copyOf(ItemStack.EMPTY, items.toArray(new ItemStack[0])));
                            }
                        } else {
                            text = switch (result.result()) {
                                case BLOCK -> config.creationFailedGraveMessage;
                                case BLOCK_CLAIM -> config.creationFailedClaimGraveMessage;
                                case ALLOW -> null;
                            };
                        }
                    } else {
                        text = switch (eventResult) {
                            case BLOCK -> config.creationFailedGraveMessage;
                            case BLOCK_CLAIM -> config.creationFailedClaimGraveMessage;
                            case BLOCK_PVP -> config.creationFailedPvPGraveMessage;
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
