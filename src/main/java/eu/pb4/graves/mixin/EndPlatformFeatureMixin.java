package eu.pb4.graves.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import eu.pb4.graves.registry.GraveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.EndPlatformFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndPlatformFeature.class)
public class EndPlatformFeatureMixin {
    @WrapOperation(method = "generate(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/util/math/BlockPos;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    private static boolean grave_dontBreak(BlockState instance, Block block, Operation<Boolean> original) {
        return original.call(instance, block) || instance.isOf(GraveBlock.INSTANCE);
    }
}
