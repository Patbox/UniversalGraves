package eu.pb4.graves.compat;


import draylar.goml.api.ClaimUtils;
import draylar.goml.api.event.ClaimEvents;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.event.GraveValidPosCheckEvent;
import eu.pb4.graves.registry.GraveBlockEntity;
import eu.pb4.graves.other.GraveUtils;
import net.minecraft.util.ActionResult;

import java.util.stream.Collectors;

public class GomlCompat {
    public static void register() {
        ClaimEvents.PERMISSION_DENIED.register((player, world, hand, pos, reason) -> {
            if (world.getBlockEntity(pos) instanceof GraveBlockEntity grave && grave.getGrave().canTakeFrom(player)) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });

        GraveValidPosCheckEvent.EVENT.register(((player, world, pos) -> {
            if (!ConfigManager.getConfig().configData.createInClaims) {
                for (var entry : ClaimUtils.getClaimsAt(world, pos).collect(Collectors.toList())) {
                    if (!entry.getValue().hasPermission(player)) {
                        return GraveUtils.BlockResult.BLOCK_CLAIM;
                    }
                }
            }
            return GraveUtils.BlockResult.ALLOW;
        }));
    }
}
