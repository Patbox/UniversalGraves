package eu.pb4.graves.mixin;

import eu.pb4.graves.GravesApi;
import eu.pb4.graves.GravesMod;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.event.PlayerGraveCreationEvent;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.grave.PositionedItemStack;
import eu.pb4.graves.other.GraveUtils;
import eu.pb4.graves.other.GravesXPCalculation;
import eu.pb4.graves.other.PlayerAdditions;
import eu.pb4.graves.registry.GraveBlock;
import eu.pb4.graves.registry.GraveBlockEntity;
import eu.pb4.graves.registry.TempBlock;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.node.TextNode;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
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
            player.sendMessage(Text.translatable("text.graves.damage_source_info",
                    Text.literal(source.name).setStyle(Style.EMPTY.withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, source.name)))
            ), false);
        }
    }

    @Inject(method = "applyDamage", at = @At("TAIL"))
    private void graves_printDamage2(DamageSource source, float amount, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayerEntity player && ((PlayerAdditions) player).graves_getPrintNextDamageSource()) {
            player.sendMessage(Text.translatable("text.graves.damage_source_info",
                    Text.literal(source.name).setStyle(Style.EMPTY.withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, source.name)))
            ), false);
        }
    }


    @Inject(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropInventory()V", shift = At.Shift.BEFORE), cancellable = true)
    private void replaceWithGrave(DamageSource source, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayerEntity player) {
            try {
                GraveUtils.createGrave(player, source, this.graves_commandKill);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
