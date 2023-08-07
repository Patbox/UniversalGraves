package eu.pb4.graves.compat;

import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import eu.pb4.graves.GravesApi;
import eu.pb4.graves.grave.GraveInventoryMask;
import eu.pb4.graves.other.VanillaInventoryMask;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class TrinketsCompat extends VanillaInventoryMask {
    public static final GraveInventoryMask INSTANCE = new TrinketsCompat();
    private static final String GROUP_TAG = "Group";
    private static final String SLOT_TAG = "Slot";

    public static void register() {
        GravesApi.registerInventoryMask(new Identifier("universal_graves", "trinkets"), INSTANCE);
    }

    @Override
    public void addToGrave(ServerPlayerEntity player, ItemConsumer consumer) {
        TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> trinkets.forEach((ref, stack) -> {
            if (stack.isEmpty() || !GravesApi.canAddItem(player, stack)) {
                return;
            }

            var dropRule = TrinketsApi.getTrinket(stack.getItem()).getDropRule(stack, ref, player);

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

            if (dropRule == TrinketEnums.DropRule.DROP) {
                var nbt = new NbtCompound();
                nbt.putString(GROUP_TAG, ref.inventory().getSlotType().getGroup());
                nbt.putString(SLOT_TAG, ref.inventory().getSlotType().getName());

                consumer.addItem(stack.copy(), ref.index(), nbt);
                inventory.setStack(ref.index(), ItemStack.EMPTY);
            }
        }));
    }

    @Override
    public boolean moveToPlayerExactly(ServerPlayerEntity player, ItemStack stack, int slot, NbtElement extraData) {
        var groupId = ((NbtCompound) extraData).getString(GROUP_TAG);
        var slotId = ((NbtCompound) extraData).getString(SLOT_TAG);

        var inventory = getInventory(player, groupId, slotId);

        if (inventory != null) {
            if (inventory.getStack(slot).isEmpty()) {
                inventory.setStack(slot, stack.copy());
                stack.setCount(0);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean moveToPlayerClosest(ServerPlayerEntity player, ItemStack stack, int slot, NbtElement data) {
        var groupId = ((NbtCompound) data).getString(GROUP_TAG);
        var slotId = ((NbtCompound) data).getString(SLOT_TAG);

        var inventory = getInventory(player, groupId, slotId);

        if (inventory != null) {
            int size = inventory.size();

            for (int i = 0; i < size; i++) {
                if (inventory.getStack(i).isEmpty()) {
                    inventory.setStack(i, stack.copy());
                    stack.setCount(0);
                    return true;
                }
            }
        }
        return false;
    }

    private TrinketInventory getInventory(ServerPlayerEntity player, String groupId, String slotId) {
        var optional = TrinketsApi.getTrinketComponent(player);
        if (optional.isPresent()) {
            var group = optional.get().getInventory().get(groupId);

            if (group != null) {
                return group.get(slotId);
            }
        }
        return null;
    }
}
