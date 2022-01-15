package eu.pb4.graves.other;

import eu.pb4.graves.GravesApi;
import eu.pb4.graves.grave.GraveInventoryMask;
import eu.pb4.graves.mixin.PlayerInventoryAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;

public class VanillaInventoryMask implements GraveInventoryMask {
    public static final VanillaInventoryMask INSTANCE = new VanillaInventoryMask();

    @Override
    public void addToGrave(ServerPlayerEntity player, ItemConsumer consumer) {
        for (int i = 0; i < player.getInventory().size(); ++i) {
            ItemStack itemStack = player.getInventory().getStack(i);
            if (GravesApi.canAddItem(player, itemStack)) {
                consumer.addItem(player.getInventory().removeStack(i), i);
            }
        }
    }

    @Override
    public boolean moveToPlayerExactly(ServerPlayerEntity player, ItemStack stack, int slot, NbtElement _unused) {
        var inventory = player.getInventory();

        if (slot > -1 && slot < inventory.size() && inventory.getStack(slot).isEmpty()) {
            inventory.setStack(slot, stack.copy());
            stack.setCount(0);
            return true;
        }

        return false;
    }

    @Override
    public boolean moveToPlayerClosest(ServerPlayerEntity player, ItemStack stack, int intended, NbtElement _unused) {
        var inventory = player.getInventory();
        if (!stack.isEmpty()) {
            int slot;
            try {
                if (stack.isDamaged()) {
                    slot = inventory.getEmptySlot();

                    if (slot >= 0) {
                        inventory.main.set(slot, stack.copy());
                        stack.setCount(0);
                        return true;
                    }
                } else {
                    int i;
                    do {
                        i = stack.getCount();
                        stack.setCount(((PlayerInventoryAccessor) inventory).callAddStack(stack));
                    } while (!stack.isEmpty() && stack.getCount() < i);
                }
            } catch (Exception e) {
                // Silence!
            }
        }

        return false;
    }
}
