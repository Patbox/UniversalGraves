package eu.pb4.graves.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.graves.other.PlayerAdditions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    
    @Inject(
            method = "respawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;setHealth(F)V"
            )
    )
    public void respawn(
            ServerPlayer serverPlayer, boolean keepAllPlayerData,
            Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir,
            @Local(name = "player") ServerPlayer player
    ) {
        ((PlayerAdditions) player).graves$tryGiveCompass();
    }
    
}
