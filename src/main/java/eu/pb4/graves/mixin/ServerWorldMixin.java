package eu.pb4.graves.mixin;

import eu.pb4.graves.registry.GraveBlock;
import eu.pb4.graves.registry.GraveBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(value = ServerWorld.class, priority = 1005)
public abstract class ServerWorldMixin extends World {
    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
    }

    @Inject(method = "canPlayerModifyAt", at = @At("HEAD"), cancellable = true)
    private void grave_disallowGraveBreaking(PlayerEntity player, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (this.getBlockEntity(pos) instanceof GraveBlockEntity grave && grave.getGrave() != null) {
            cir.setReturnValue(grave.getGrave().canTakeFrom(player));
        }
    }

    @Inject(method = {"method_29204", "method_29201"}, at = @At("HEAD"), cancellable = true)
    private static void grave_dontBreak(ServerWorld serverWorld, BlockPos pos, CallbackInfo ci) {
        if (serverWorld.getBlockState(pos).getBlock() == GraveBlock.INSTANCE) {
            ci.cancel();
        }
    }
}
