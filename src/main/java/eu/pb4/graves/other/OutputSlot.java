package eu.pb4.graves.other;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class OutputSlot extends Slot {
    private final boolean canTake;

    public OutputSlot(Inventory inventory, int index, int x, int y, boolean canTake) {
        super(inventory, index, x, y);
        this.canTake = canTake;
    }

    @Override
    public boolean canTakeItems(PlayerEntity playerEntity) {
        return this.canTake;
    }

    @Override
    public ItemStack takeStack(int amount) {
        return this.canTake ? super.takeStack(amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack takeStackRange(int min, int max, PlayerEntity player) {
        return this.canTake ? super.takeStackRange(min, max, player) : ItemStack.EMPTY;
    }

    @Override
    public boolean canTakePartial(PlayerEntity player) {
        return false;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }
}
