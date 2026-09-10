package eu.pb4.graves.mixin;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.registry.GraveCompassItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import eu.pb4.graves.other.PlayerAdditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements PlayerAdditions {
    public ServerPlayerMixin(Level world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Unique
    private long graves$location = -1;

    @Unique
    private boolean graves$hasCompass = false;

    @Unique
    private boolean graves$isInvulnerable = false;

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void graves$loadNbt(ValueInput view, CallbackInfo ci) {
        this.graves$location = view.getLongOr("LastGraveId", -1);
        this.graves$hasCompass = view.getBooleanOr("HasGraveCompass", false);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void graves$writeNbt(ValueOutput view, CallbackInfo ci) {
        if (this.graves$location != -1) {
            view.putLong("LastGraveId", this.graves$location);
        }

         view.putBoolean("HasGraveCompass", this.graves$hasCompass);
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void graves$copyDate(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        this.graves$hasCompass = ((ServerPlayerMixin) (Object) oldPlayer).graves$hasCompass;
        this.graves$location = ((PlayerAdditions) oldPlayer).graves$lastGrave();
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void graves$setupThings(DamageSource source, CallbackInfo ci) {
        this.graves$location = -1;
        this.graves$hasCompass = false;
    }

    @Inject(method = "initInventoryMenu", at = @At("TAIL"))
    private void graves$onSpawn(CallbackInfo ci) {
        if (this.graves$location != -1 && !this.graves$hasCompass && ConfigManager.getConfig().interactions.giveGraveCompass) {
            this.getInventory().placeItemBackInInventory(GraveCompassItem.create(this.graves$location, false), Prediction.SERVER_ONLY);
            this.graves$hasCompass = true;
        }
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void graves$isInvulnerableTo(ServerLevel world, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
         if (this.graves$isInvulnerable) {
             cir.setReturnValue(true);
         }
    }

    @Override
    public Component graves$lastDeathCause() {
        return null;
    }

    @Override
    public long graves$lastGrave() {
        return this.graves$location;
    }

    @Override
    public void graves$setLastGrave(long id) {
        this.graves$location = id;
    }

    @Override
    public void graves$setInvulnerable(boolean value) {
        this.graves$isInvulnerable = value;
    }
}
