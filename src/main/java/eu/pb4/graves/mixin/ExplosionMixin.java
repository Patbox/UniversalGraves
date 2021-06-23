package eu.pb4.graves.mixin;

import eu.pb4.graves.grave.GraveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Explosion.class)
public class ExplosionMixin {
    @ModifyVariable(method = "affectWorld", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;", ordinal = 0))
    private BlockState dontBreakGraves(BlockState state) {
        if (state.getBlock() == GraveBlock.INSTANCE) {
            return Blocks.AIR.getDefaultState();
        }
        return state;
    }
}

