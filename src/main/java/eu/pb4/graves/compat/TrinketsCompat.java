package eu.pb4.graves.compat;

import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;

public class TrinketsCompat {
    public static void register() {
        PlayerGraveItemsEvent.EVENT.register((player, items) -> {
            TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> trinkets.forEach((ref, stack) -> {
                if (stack.isEmpty()) {
                    return;
                }

                TrinketEnums.DropRule dropRule = TrinketsApi.getTrinket(stack.getItem()).getDropRule(stack, ref, player);

                dropRule = TrinketDropCallback.EVENT.invoker().drop(dropRule, stack, ref, player);

                TrinketInventory inventory = ref.inventory();

                if (dropRule == TrinketEnums.DropRule.DEFAULT) {
                    dropRule = inventory.getSlotType().getDropRule();
                }

                if (dropRule == TrinketEnums.DropRule.DEFAULT) {
                    if (EnchantmentHelper.hasVanishingCurse(stack)) {
                        dropRule = TrinketEnums.DropRule.DESTROY;
                    } else {
                        dropRule = TrinketEnums.DropRule.DROP;
                    }
                }

                switch (dropRule) {
                    case DROP:
                        items.add(stack);

                    case DESTROY:
                        inventory.setStack(ref.index(), ItemStack.EMPTY);
                        break;
                    default:
                        break;
                }
            }));

        });
    }
}
