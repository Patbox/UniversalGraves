package eu.pb4.graves.compat;


import draylar.goml.api.event.ClaimEvents;
import eu.pb4.graves.registry.GraveBlockEntity;
import net.minecraft.util.ActionResult;

public class GomlCompat {
    public static void register() {
        ClaimEvents.PERMISSION_DENIED.register((player, world, hand, pos, reason) -> {
            if (world.getBlockEntity(pos) instanceof GraveBlockEntity grave && grave.getGrave() != null && grave.getGrave().canTakeFrom(player)) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });

    }
}
