package eu.pb4.graves.mixin;

import eu.pb4.graves.registry.AbstractGraveBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow @Final protected ServerPlayerEntity player;

    @Shadow protected ServerWorld world;

    @Inject(
            method = "interactItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;", shift = At.Shift.BEFORE),
            cancellable = true
    )
    private void preventItemUse(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player.raycast(player.getBlockInteractionRange(), 0, false) instanceof BlockHitResult cast && cast.getType() != HitResult.Type.MISS) {
            var blockState = world.getBlockState(cast.getBlockPos());
            if (blockState.getBlock() instanceof AbstractGraveBlock) {
                cir.setReturnValue(ActionResult.PASS);
            }
        }
    }
}