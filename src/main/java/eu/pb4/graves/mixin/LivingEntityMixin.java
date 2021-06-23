package eu.pb4.graves.mixin;

import com.google.common.collect.ImmutableList;
import eu.pb4.graves.compat.PlayerGraveItemsEvent;
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
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropInventory()V", shift = At.Shift.BEFORE), cancellable = true)
    private void replaceWithGrave(DamageSource source, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayerEntity player) {
            if (player.getServerWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
                return;
            }

            try {

                Config config = ConfigManager.getConfig();
                Text text = null;
                Map<String, Text> placeholders = Collections.emptyMap();

                if (!config.configData.createGravesFromPvP && source.getAttacker() instanceof PlayerEntity) {
                    if (config.configData.displayCreationFailedPvPGraveMessage) {
                        text = config.creationFailedPvPGraveMessage;
                    }
                } else {
                    BlockPos gravePos = GraveUtils.findGravePosition(player.getServerWorld(), player.getBlockPos(), TagRegistry.block(GraveUtils.REPLACEABLE_TAG));

                    if (gravePos != null) {
                        List<ItemStack> items = new ArrayList<>();

                        for (DefaultedList<ItemStack> list : ImmutableList.of(player.getInventory().main, player.getInventory().armor, player.getInventory().offHand)) {
                            for (ItemStack stack : list) {
                                if (!stack.isEmpty()) {
                                    items.add(stack);
                                }
                            }
                        }

                        PlayerGraveItemsEvent.EVENT.invoker().modifyItems(player, items);

                        if (items.size() == 0) {
                            return;
                        }
                        BlockState blockState = player.getServerWorld().getBlockState(gravePos);
                        player.getServerWorld().setBlockState(gravePos, GraveBlock.INSTANCE.getDefaultState().with(Properties.ROTATION, player.getRandom().nextInt(15)));
                        BlockEntity entity = player.getServerWorld().getBlockEntity(gravePos);

                        if (entity instanceof GraveBlockEntity grave) {
                            for (int i = 0; i < player.getInventory().size(); ++i) {
                                ItemStack itemStack = player.getInventory().getStack(i);
                                if (!itemStack.isEmpty() && EnchantmentHelper.hasVanishingCurse(itemStack)) {
                                    player.getInventory().removeStack(i);
                                }
                            }

                            int i = player.experienceLevel * 7;
                            grave.setGrave(player.getGameProfile(), items, i > 100 ? 100 : i, source.getDeathMessage(player), blockState);
                            player.experienceLevel = 0;
                            if (config.configData.displayCreatedGraveMessage) {
                                text = config.createdGraveMessage;
                                placeholders = grave.info.getPlaceholders();
                            }
                            ci.cancel();
                        } else {
                            if (config.configData.displayCreationFailedGraveMessage) {
                                text = config.creationFailedGraveMessage;
                                placeholders = Map.of("position", new LiteralText("" + player.getBlockPos().toShortString()));
                            }
                        }
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
