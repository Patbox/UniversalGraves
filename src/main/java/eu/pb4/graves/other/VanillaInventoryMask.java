package eu.pb4.graves.other;

import eu.pb4.graves.GravesApi;
import eu.pb4.graves.grave.GraveInventoryMask;
import eu.pb4.graves.mixin.PlayerInventoryAccessor;
import eu.pb4.graves.model.ModelTags;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class VanillaInventoryMask implements GraveInventoryMask {
    public static final VanillaInventoryMask INSTANCE = new VanillaInventoryMask();

    @Override
    public void addToGrave(ServerPlayerEntity player, ItemConsumer consumer) {
        var inventory = player.getInventory();
        var size = inventory.size();
        for (int slot = 0; slot < size; slot++) {
            ItemStack itemStack = inventory.getStack(slot);
            if (GravesApi.canAddItem(player, itemStack)) {
                inventory.setStack(slot, ItemStack.EMPTY);
                consumer.addItem(itemStack, slot, createTags(player, inventory, slot, itemStack));
            }
        }

        ItemStack itemStack = player.playerScreenHandler.getCursorStack();
        if (GravesApi.canAddItem(player, itemStack)) {
            consumer.addItem(itemStack.copy(), -1);
            player.playerScreenHandler.setCursorStack(ItemStack.EMPTY);
        }
    }

    private Identifier[] createTags(ServerPlayerEntity player, PlayerInventory inventory, int slot, ItemStack stack) {
        if (slot == inventory.getSelectedSlot()) {
            return new Identifier[]{ModelTags.EQUIPMENT_MAIN_HAND};
        }

        return switch (slot) {
            case PlayerInventory.OFF_HAND_SLOT -> new Identifier[]{ModelTags.EQUIPMENT_OFFHAND_HAND};
            case PlayerInventory.MAIN_SIZE -> new Identifier[]{ModelTags.EQUIPMENT_BOOTS};
            case PlayerInventory.MAIN_SIZE + 1 -> new Identifier[]{ModelTags.EQUIPMENT_LEGGINGS};
            case PlayerInventory.MAIN_SIZE + 2 -> new Identifier[]{ModelTags.EQUIPMENT_CHESTPLATE};
            case PlayerInventory.MAIN_SIZE + 3 -> new Identifier[]{ModelTags.EQUIPMENT_HELMET};
            default -> new Identifier[0];
        };
    }

    @Override
    public boolean moveToPlayerExactly(ServerPlayerEntity player, ItemStack stack, int slot, NbtElement extraData) {
        var inventory = player.getInventory();
        if (slot > -1 && slot < inventory.size() && inventory.getStack(slot).isEmpty()) {
            inventory.setStack(slot, stack.copy());
            stack.setCount(0);
            return true;
        }

        return false;
    }

    @Override
    public boolean moveToPlayerClosest(ServerPlayerEntity player, ItemStack stack, int intended, NbtElement extraData) {
        var inventory = player.getInventory();
        if (!stack.isEmpty()) {
            int slot;
            try {
                if (stack.isDamaged()) {
                    slot = inventory.getEmptySlot();

                    if (slot >= 0) {
                        inventory.getMainStacks().set(slot, stack.copy());
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
