package eu.pb4.graves.compat;

import eu.pb4.graves.GravesApi;
import net.minecraft.util.ActionResult;

public class SaveGearOnDeathCompat {
    public static void register() {
        GravesApi.ADD_ITEM_EVENT.register((player, item) -> {
            var index = player.getInventory().getMainStacks().indexOf(item);

            return index > 8 ? ActionResult.PASS : ActionResult.FAIL;
        });
    }
}
