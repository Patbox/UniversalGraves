package eu.pb4.graves.mixin;

import eu.pb4.graves.grave.GraveBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {
    @Inject(method = "canPlayerModifyAt", at = @At("HEAD"), cancellable = true)
    private void disallowGraveBreaking(PlayerEntity player, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (this.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
            cir.setReturnValue(grave.info.canTakeFrom(player));
        }
    }


    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DimensionType dimensionType, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, dimensionType, profiler, isClient, debugWorld, seed);
    }
}
