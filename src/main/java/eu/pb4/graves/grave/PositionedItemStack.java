package eu.pb4.graves.grave;

import eu.pb4.graves.GravesApi;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record PositionedItemStack(ItemStack stack, int slot, @Nullable GraveInventoryMask inventoryMask, @Nullable
                                  NbtElement optionalData) {
    public static final PositionedItemStack EMPTY = new PositionedItemStack(ItemStack.EMPTY, -1, null, null);

    private static final String ITEM_TAG = "Item";
    private static final String MASK_TAG = "Mask";
    private static final String SLOT_TAG = "Slot";
    private static final String DATA_TAG = "Data";

    public NbtCompound toNbt() {
        var nbt = new NbtCompound();
        nbt.put(ITEM_TAG, stack.writeNbt(new NbtCompound()));
        nbt.putString(MASK_TAG, GravesApi.getInventoryMaskId(inventoryMask).toString());
        nbt.putInt(SLOT_TAG, slot);
        if (optionalData != null) {
            nbt.put(DATA_TAG, optionalData);
        }
        return nbt;
    }

    public static PositionedItemStack fromNbt(NbtCompound nbt) {
        return new PositionedItemStack(
                ItemStack.fromNbt(nbt.getCompound(ITEM_TAG)),
                nbt.getInt(SLOT_TAG),
                GravesApi.getDefaultedInventoryMask(Identifier.tryParse(nbt.getString(MASK_TAG))),
                nbt.get(DATA_TAG)
        );
    }

    public boolean isEmpty() {
        return this.stack.isEmpty();
    }
}