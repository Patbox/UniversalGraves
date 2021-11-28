package eu.pb4.graves.compat;

import eu.pb4.graves.event.PlayerGraveItemAddedEvent;
import eu.pb4.graves.event.PlayerGraveItemsEvent;
import eu.pb4.graves.other.GraveUtils;
import me.lizardofoz.inventorio.api.InventorioAPI;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.util.ActionResult;

public class InventorioCompat {
    public static void register() {
        PlayerGraveItemsEvent.EVENT.register((player, items) -> {
            var inv = InventorioAPI.getInventoryAddon(player);

            for (int i = 0; i < inv.size(); i++) {
                var stack = inv.getStack(i);
                if (!stack.isEmpty()
                        && PlayerGraveItemAddedEvent.EVENT.invoker().canAddItem(player, stack) != ActionResult.FAIL
                        && !GraveUtils.hasSkippedEnchantment(stack)
                        && !EnchantmentHelper.hasVanishingCurse(stack)
                ) {
                    items.add(inv.removeStack(i));
                }
            }
        });
    }
}