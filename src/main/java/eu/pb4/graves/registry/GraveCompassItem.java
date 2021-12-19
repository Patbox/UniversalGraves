package eu.pb4.graves.registry;

import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.other.Location;
import eu.pb4.graves.other.PlayerAdditions;
import eu.pb4.polymer.api.item.PolymerItem;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GraveCompassItem extends Item implements PolymerItem {
    public static Item INSTANCE = new GraveCompassItem();

    public GraveCompassItem() {
        super(new Settings().maxCount(1));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.COMPASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayerEntity && !stack.isEmpty()) {
            if (stack.hasNbt() && stack.getNbt().contains("Location", NbtElement.COMPOUND_TYPE)) {
                var location = Location.fromNbt((NbtCompound) stack.getNbt().get("Location"));
                var grave = GraveManager.INSTANCE.getByLocation(location);

                if (grave == null) {
                    stack.setCount(0);
                }
            } else {
                var location = ((PlayerAdditions) entity).graves_lastGrave();
                if (location != null) {
                    stack.getOrCreateNbt().put("Location", location.writeNbt(new NbtCompound()));
                } else {
                    stack.setCount(0);
                }
            }
        }
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var clientStack = PolymerItem.super.getPolymerItemStack(itemStack, player);
        if (itemStack.hasNbt() && itemStack.getNbt().contains("Location", NbtElement.COMPOUND_TYPE)) {
            var location = Location.fromNbt((NbtCompound) itemStack.getNbt().get("Location"));
            clientStack.getOrCreateNbt().putString("LodestoneDimension", location.world().toString());
            var pos = new NbtCompound();
            pos.putInt("X", location.x());
            pos.putInt("Y", location.y());
            pos.putInt("Z", location.z());
            clientStack.getOrCreateNbt().put("LodestonePos", pos);
        }
        clientStack.getOrCreateNbt().putBoolean("LodestoneTracked", true);
        return clientStack;
    }
}
