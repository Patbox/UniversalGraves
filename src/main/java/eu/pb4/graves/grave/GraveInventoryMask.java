package eu.pb4.graves.grave;

import eu.pb4.graves.GravesApi;
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
            this.addItem(stack, slot, null);
        }

        void addItem(ItemStack stack, int slot, @Nullable NbtElement nbtElement);
    }
}
