package eu.pb4.graves.mixin;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.registry.GraveCompassItem;
import eu.pb4.graves.other.Location;
import eu.pb4.graves.other.PlayerAdditions;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements PlayerAdditions {

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Shadow protected abstract void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition);

    @Unique
    private Location graves_location = null;

    @Unique
    private boolean graves_hasCompass = false;

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void graves_loadNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("GraveLocation", NbtElement.COMPOUND_TYPE)) {
            this.graves_location = Location.fromNbt(nbt.getCompound("GraveLocation"));
        }

        this.graves_hasCompass = nbt.getBoolean("HasGraveCompass");
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void graves_writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (this.graves_location != null) {
            nbt.put("GraveLocation", this.graves_location.writeNbt(new NbtCompound()));
        }

         nbt.putBoolean("HasGraveCompass", this.graves_hasCompass);
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void graves_copyDate(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.graves_hasCompass =((ServerPlayerEntityMixin) (Object) oldPlayer).graves_hasCompass;
        this.graves_location = ((PlayerAdditions) oldPlayer).graves_lastGrave();

        if (this.graves_location != null && !this.graves_hasCompass && ConfigManager.getConfig().configData.giveGraveCompass) {
            var compass = new ItemStack(GraveCompassItem.INSTANCE);
            compass.getOrCreateNbt().put("Location", this.graves_location.writeNbt(new NbtCompound()));
            this.getInventory().insertStack(compass);
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void grave_setupThings(DamageSource source, CallbackInfo ci) {
        this.graves_location = null;
        this.graves_hasCompass = false;
    }

    @Override
    public Text graves_lastDeathCause() {
        return null;
    }

    @Override
    public Location graves_lastGrave() {
        return this.graves_location;
    }

    @Override
    public void graves_setLastGrave(Location location) {
        this.graves_location = location;
    }
}
