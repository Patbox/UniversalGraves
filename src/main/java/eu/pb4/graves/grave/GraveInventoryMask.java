package eu.pb4.graves.grave;

import eu.pb4.graves.GravesApi;
import eu.pb4.graves.model.ModelTags;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface GraveInventoryMask {
    void addToGrave(ServerPlayerEntity player, ItemConsumer itemStackConsumer);

    boolean moveToPlayerExactly(ServerPlayerEntity player, ItemStack stack, int slot, @Nullable NbtElement optionalData);
    boolean moveToPlayerClosest(ServerPlayerEntity player, ItemStack stack, int slot, @Nullable NbtElement optionalData);

    default Identifier getId() {
        return GravesApi.getInventoryMaskId(this);
    }


    interface ItemConsumer {
        default void addItem(ItemStack stack, int slot) {
            this.addItem(stack, slot, null, new Identifier[0]);
        }
        default void addItem(ItemStack stack, int slot, Identifier... tags) {
            this.addItem(stack, slot, null, tags);
        }

        default void addItem(ItemStack stack, int slot, @Nullable NbtElement nbtElement) {
            this.addItem(stack, slot, nbtElement, new Identifier[0]);
        };

        void addItem(ItemStack stack, int slot, @Nullable NbtElement nbtElement, Identifier... tags);
    }
}
