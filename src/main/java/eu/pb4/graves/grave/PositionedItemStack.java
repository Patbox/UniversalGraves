package eu.pb4.graves.grave;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.MapCodec;
import eu.pb4.graves.GravesApi;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public record PositionedItemStack(ItemStack stack, int slot, @Nullable GraveInventoryMask inventoryMask, @Nullable
                                  NbtElement optionalData, Set<Identifier> tags) {
    public static final PositionedItemStack EMPTY = new PositionedItemStack(ItemStack.EMPTY, -1, null, null, Set.of());

    private static final String ITEM_TAG = "Item";
    private static final String MASK_TAG = "Mask";
    private static final String SLOT_TAG = "Slot";
    private static final String DATA_TAG = "Data";
    private static final String TAGS_TAG = "Tags";

    public NbtCompound toNbt(RegistryWrapper.WrapperLookup lookup) {
        var nbt = new NbtCompound();
        nbt.put(ITEM_TAG, ItemStack.OPTIONAL_CODEC, lookup.getOps(NbtOps.INSTANCE), stack);
        nbt.putString(MASK_TAG, GravesApi.getInventoryMaskId(inventoryMask).toString());
        nbt.putInt(SLOT_TAG, slot);
        if (optionalData != null) {
            nbt.put(DATA_TAG, optionalData);
        }
        if (!this.tags.isEmpty()) {
            var list = new NbtList();
            for (var tag : tags) {
                list.add(NbtString.of(tag.toString()));
            }
            nbt.put(TAGS_TAG, list);
        }

        return nbt;
    }

    public static PositionedItemStack fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup, DataFixer dataFixer, int dataVersion, int currentDataVersion) {
        var stackData = nbt.getCompoundOrEmpty(ITEM_TAG);
        if (dataVersion < currentDataVersion) {
            stackData = (NbtCompound) dataFixer.update(TypeReferences.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, stackData), dataVersion, currentDataVersion).getValue();
        }

        return new PositionedItemStack(
                stackData.decode(MapCodec.assumeMapUnsafe(ItemStack.OPTIONAL_CODEC)).orElse(ItemStack.EMPTY),
                nbt.getInt(SLOT_TAG, 0),
                GravesApi.getDefaultedInventoryMask(Identifier.tryParse(nbt.getString(MASK_TAG, ""))),
                nbt.get(DATA_TAG),
                nbt.contains(TAGS_TAG) ? decodeSet(nbt.getListOrEmpty(TAGS_TAG)) : Set.of()
        );
    }

    private static Set<Identifier> decodeSet(NbtList list) {
        if (list.isEmpty()) {
            return Set.of();
        }

        var set = new HashSet<Identifier>();
        for (var entry : list) {
            set.add(Identifier.tryParse(entry.asString().get()));
        }

        return set;
    }

    public boolean isEmpty() {
        return this.stack.isEmpty();
    }
}