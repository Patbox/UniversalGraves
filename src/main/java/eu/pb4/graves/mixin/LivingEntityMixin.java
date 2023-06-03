package eu.pb4.graves.mixin;

import eu.pb4.graves.other.GraveUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
            try {
                GraveUtils.createGrave(player, source, this.graves_commandKill);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
