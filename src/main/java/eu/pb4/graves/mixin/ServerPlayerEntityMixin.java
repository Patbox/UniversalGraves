package eu.pb4.graves.mixin;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.registry.GraveCompassItem;
import eu.pb4.graves.other.PlayerAdditions;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements PlayerAdditions {
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique
    private long graves$location = -1;

    @Unique
    private boolean graves$hasCompass = false;

    @Unique
    private boolean graves$isInvulnerable = false;

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void graves$loadNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("LastGraveId")) {
            this.graves$location = nbt.getLong("LastGraveId", -1);
        }

        this.graves$hasCompass = nbt.getBoolean("HasGraveCompass", false);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void graves$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (this.graves$location != -1) {
            nbt.putLong("LastGraveId", this.graves$location);
        }

         nbt.putBoolean("HasGraveCompass", this.graves$hasCompass);
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void graves$copyDate(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.graves$hasCompass = ((ServerPlayerEntityMixin) (Object) oldPlayer).graves$hasCompass;
        this.graves$location = ((PlayerAdditions) oldPlayer).graves$lastGrave();
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void graves$setupThings(DamageSource source, CallbackInfo ci) {
        this.graves$location = -1;
        this.graves$hasCompass = false;
    }

    @Inject(method = "onSpawn", at = @At("TAIL"))
    private void graves$onSpawn(CallbackInfo ci) {
        if (this.graves$location != -1 && !this.graves$hasCompass && ConfigManager.getConfig().interactions.giveGraveCompass) {
            this.getInventory().offerOrDrop(GraveCompassItem.create(this.graves$location, false));
            this.graves$hasCompass = true;
        }
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void graves$isInvulnerableTo(ServerWorld world, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
         if (this.graves$isInvulnerable) {
             cir.setReturnValue(true);
         }
    }

    @Override
    public Text graves$lastDeathCause() {
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
