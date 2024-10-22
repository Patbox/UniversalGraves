package eu.pb4.graves.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pb4.graves.registry.GraveBlock;
import eu.pb4.graves.registry.GravesRegistry;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com/simibubi/create/content/kinetics/base/BlockBreakingKineticBlockEntity")
public class CreateBlockBreakingKineticBlockEntity {
    @ModifyReturnValue(method = "isBreakable(Lnet/minecraft/block/BlockState;F)Z", at = @At("RETURN"), require = 0)
    private static boolean graves$makeUnbreakable(boolean original, BlockState state, float f) {
        return original && !state.isOf(GravesRegistry.GRAVE_BLOCK);
    }
}
