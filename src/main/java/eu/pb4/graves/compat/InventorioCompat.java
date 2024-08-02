package eu.pb4.graves.compat;

import de.rubixdev.inventorio.api.InventorioAPI;
import eu.pb4.graves.GravesApi;
import eu.pb4.graves.grave.GraveInventoryMask;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;


public record InventorioCompat() implements GraveInventoryMask {
    public static final GraveInventoryMask INSTANCE = new InventorioCompat();

    public static void register() {
        GravesApi.registerInventoryMask(Identifier.of("universal_graves", "inventorio"), INSTANCE);
    }

    @Override
    public void addToGrave(ServerPlayerEntity player, ItemConsumer consumer) {
        var inv = InventorioAPI.getInventoryAddon(player);

        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStack(i);
            if (GravesApi.canAddItem(player, stack)) {
                consumer.addItem(inv.removeStack(i), i);
            }
        }
    }

    @Override
    public boolean moveToPlayerExactly(ServerPlayerEntity player, ItemStack stack, int slot, NbtElement _unused) {
        var inventory = player.getInventory();

        if (inventory.getStack(slot).isEmpty()) {
            inventory.setStack(slot, stack);
            return true;
        }

        return false;
    }

    @Override
    public boolean moveToPlayerClosest(ServerPlayerEntity player, ItemStack stack, int intended, NbtElement _unused) {
        var inventory = InventorioAPI.getInventoryAddon(player);
        if (!stack.isEmpty()) {
            try {
                stack.setCount(inventory.addStack(stack).getCount());
                return true;
            } catch (Exception e) {
                // Silence!
            }
        }

        return false;
    }
}