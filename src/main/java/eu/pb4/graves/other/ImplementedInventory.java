package eu.pb4.graves.other;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.Iterator;

/**
 * A simple {@code Inventory} implementation with only default methods + an item list getter.
 *
 * Originally by Juuz
 */
public interface ImplementedInventory extends Inventory {

    DefaultedList<ItemStack> getItems();


    static ImplementedInventory of(DefaultedList<ItemStack> items, Runnable markDirty) {
        return new ImplementedInventory() {
            @Override
            public DefaultedList<ItemStack> getItems() {
                return items;
            }

            @Override
            public void markDirty() {
                markDirty.run();
            }
        };
    }


    static ImplementedInventory ofSize(int size) {
        return of(DefaultedList.ofSize(size, ItemStack.EMPTY), () -> {});
    }


    @Override
    default int size() {
        return getItems().size();
    }


    @Override
    default boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }


    @Override
    default ItemStack getStack(int slot) {
        return getItems().get(slot);
    }


    @Override
    default ItemStack removeStack(int slot, int count) {
        ItemStack result = Inventories.splitStack(this.getItems(), slot, count);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }


    @Override
    default ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.getItems(), slot);
    }


    @Override
    default void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
    }

    @Override
    default void clear() {
        getItems().clear();
    }


    void markDirty();


    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    default boolean canInsert(ItemStack stack) {
        boolean bl = false;
        Iterator var3 = this.getItems().iterator();

        while(var3.hasNext()) {
            ItemStack itemStack = (ItemStack)var3.next();
            if (itemStack.isEmpty() || this.canCombine(itemStack, stack) && itemStack.getCount() < itemStack.getMaxCount()) {
                bl = true;
                break;
            }
        }

        return bl;
    }

    default ItemStack addStack(ItemStack stack) {
        ItemStack itemStack = stack.copy();
        this.addToExistingSlot(itemStack);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.addToNewSlot(itemStack);
            return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack;
        }
    }

    default void addToExistingSlot(ItemStack stack) {
        for(int i = 0; i < this.size(); ++i) {
            ItemStack itemStack = this.getStack(i);
            if (this.canCombine(itemStack, stack)) {
                this.transfer(stack, itemStack);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
    }

    default void addToNewSlot(ItemStack stack) {
        for(int i = 0; i < this.size(); ++i) {
            ItemStack itemStack = this.getStack(i);
            if (itemStack.isEmpty()) {
                this.setStack(i, stack.copy());
                stack.setCount(0);
                return;
            }
        }
    }

    default void transfer(ItemStack source, ItemStack target) {
        int i = Math.min(this.getMaxCountPerStack(), target.getMaxCount());
        int j = Math.min(source.getCount(), i - target.getCount());
        if (j > 0) {
            target.increment(j);
            source.decrement(j);
            this.markDirty();
        }

    }

   default boolean canCombine(ItemStack one, ItemStack two) {
        return one.isEmpty() == two.isEmpty() && ItemStack.canCombine(one, two);
    }
}