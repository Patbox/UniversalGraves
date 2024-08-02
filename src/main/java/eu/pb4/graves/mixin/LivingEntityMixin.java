package eu.pb4.graves.mixin;

import eu.pb4.graves.GravesMod;
import eu.pb4.graves.other.GraveUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropInventory()V", shift = At.Shift.BEFORE))
    private void replaceWithGrave(ServerWorld world, DamageSource damageSource, CallbackInfo ci) {
        if (((Object) this) instanceof ServerPlayerEntity player) {
            try {
                GraveUtils.createGrave(player, world, damageSource);
            } catch (Throwable e) {
                player.sendMessage(Text.literal("Failed to create a grave due to an exception! See logs for more information and report it!").formatted(Formatting.RED));
                player.sendMessage(Text.literal(e.toString()).formatted(Formatting.RED));
                GravesMod.LOGGER.error("Failed to create a grave due to an exception!", e);
            }
        }
    }
}
