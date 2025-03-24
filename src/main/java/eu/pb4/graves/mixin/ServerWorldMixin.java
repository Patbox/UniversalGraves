package eu.pb4.graves.mixin;

import eu.pb4.graves.registry.GraveBlock;
import eu.pb4.graves.registry.GraveBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.registry.RegistryKey;
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
    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Inject(method = "canEntityModifyAt", at = @At("HEAD"), cancellable = true)
    private void grave_disallowGraveBreaking(Entity entity, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (this.getBlockEntity(pos) instanceof GraveBlockEntity grave && grave.getGrave() != null) {
            cir.setReturnValue(entity instanceof PlayerEntity player && grave.getGrave().hasAccess(player));
        }
    }
}
