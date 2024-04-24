package eu.pb4.graves.registry;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.other.PlayerAdditions;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.client.item.TooltipType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class GraveCompassItem extends Item implements PolymerItem {
    public static Item INSTANCE = new GraveCompassItem();

    public GraveCompassItem() {
        super(new Settings().maxCount(1));
    }

    public static ItemStack create(long graveId, boolean toVanilla) {
        var stack = new ItemStack(INSTANCE);
        stack.set(GraveCompassComponent.TYPE, new GraveCompassComponent(graveId, toVanilla));
        return stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity serverPlayerEntity && ConfigManager.getConfig().interactions.useDeathCompassToOpenGui && stack.contains(GraveCompassComponent.TYPE)) {
            Grave grave = GraveManager.INSTANCE.getId(stack.get(GraveCompassComponent.TYPE).graveId());
            grave.openUi(serverPlayerEntity, false, false);
        }
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.COMPASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayerEntity player && !stack.isEmpty()) {
            if (stack.contains(GraveCompassComponent.TYPE)) {
                var grave = GraveManager.INSTANCE.getId(stack.get(GraveCompassComponent.TYPE).graveId());

                if (grave == null) {
                    var count = stack.getCount();
                    stack.setCount(0);

                    if (stack.get(GraveCompassComponent.TYPE).convertToVanilla()) {
                        player.giveItemStack(new ItemStack(Items.COMPASS, count));
                    }
                }
            } else {
                var graveId = ((PlayerAdditions) entity).graves$lastGrave();
                if (graveId != -1) {
                    stack.set(GraveCompassComponent.TYPE, new GraveCompassComponent(graveId, false));
                } else {
                    stack.setCount(0);
                }
            }
        }
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType context, @Nullable ServerPlayerEntity player) {
        var clientStack = PolymerItem.super.getPolymerItemStack(itemStack, context, player);
        if (player != null && itemStack.contains(GraveCompassComponent.TYPE)) {
            var grave = GraveManager.INSTANCE.getId(itemStack.get(GraveCompassComponent.TYPE).graveId());
            if (grave != null) {
                clientStack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.of(grave.getLocation().asGlobalPos()), true));
            }
        } else {
            clientStack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.empty(), true));
        }
        return clientStack;
    }
}
