package eu.pb4.graves.registry;

import eu.pb4.graves.GraveNetworking;
import eu.pb4.polymer.api.client.PolymerClientDecoded;
import eu.pb4.polymer.api.client.PolymerKeepModel;
import eu.pb4.polymer.api.item.PolymerItem;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class VisualGraveBlockItem extends BlockItem implements PolymerItem, PolymerClientDecoded, PolymerKeepModel {
    public static final Item INSTANCE = new VisualGraveBlockItem();

    public VisualGraveBlockItem() {
        super(VisualGraveBlock.INSTANCE, new Settings());
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return player != null && GraveNetworking.canReceive(player.networkHandler) ? this : Items.STONE_BRICK_WALL;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        /*if (player != null && GraveNetworking.canReceive(player.networkHandler)) {
            return itemStack;
        }*/

        return PolymerItem.super.getPolymerItemStack(itemStack, player);
    }
}
