package eu.pb4.graves.compat;

import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.TrinketSlotReference;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.graves.GravesApi;
import eu.pb4.graves.grave.GraveInventoryMask;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record TrinketsUpdatedCompat() implements GraveInventoryMask {
    private static final String INVENTORY_TAG = "slot_ref";

    public static void register() {
        GravesApi.registerInventoryMask(Identifier.fromNamespaceAndPath("universal_graves", "trinkets_updated"), new TrinketsUpdatedCompat());
    }

    @Override
    public void addToGrave(ServerPlayer player, ItemConsumer consumer) {
        TrinketsApi.getAttachment(player).forEachDroppable((slot, stack) -> {
            if (stack.isEmpty() || !GravesApi.canAddItem(player, stack)) {
                return;
            }
            var nbt = new CompoundTag();
            nbt.store(INVENTORY_TAG, TrinketSlotReference.CODEC, slot.reference());
            consumer.addItem(stack.copy(), 0, nbt);
            slot.set(ItemStack.EMPTY);
        });
    }

    @Override
    public boolean moveToPlayerExactly(ServerPlayer player, ItemStack stack, int slot, Tag extraData) {
        var inventoryId = ((CompoundTag) extraData).read(INVENTORY_TAG, TrinketSlotReference.CODEC);

        if (inventoryId.isEmpty()) {
            return false;
        }

        var access = TrinketsApi.getAttachment(player).getSlotAccess(inventoryId.orElseThrow());

        if (access != null) {
            if (access.get().isEmpty()) {
                access.set(stack.copyAndClear());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean moveToPlayerClosest(ServerPlayer player, ItemStack stack, int slot, Tag data) {
        var inventoryId = ((CompoundTag) data).getStringOr(INVENTORY_TAG, "");

        var inventory = TrinketsApi.getAttachment(player).getInventory(inventoryId);

        if (inventory != null) {
            int size = inventory.getContainerSize();

            for (int i = 0; i < size; i++) {
                if (inventory.getItem(i).isEmpty()) {
                    inventory.setItem(i, stack.copyAndClear());
                    return true;
                }
            }
        }
        return false;
    }
}