package eu.pb4.graves.registry;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.other.PlayerAdditions;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Objects;
import java.util.Optional;

public class GraveCompassItem extends Item implements PolymerItem {
    public GraveCompassItem(Settings settings) {
        super(settings.maxCount(1));
    }

    public static ItemStack create(long graveId, boolean toVanilla) {
        var stack = new ItemStack(GravesRegistry.GRAVE_COMPASS_ITEM);
        stack.set(GraveCompassComponent.TYPE, new GraveCompassComponent(graveId, toVanilla));
        return stack;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity serverPlayerEntity && ConfigManager.getConfig().interactions.useDeathCompassToOpenGui && stack.contains(GraveCompassComponent.TYPE)) {
            Grave grave = GraveManager.INSTANCE.getId(stack.get(GraveCompassComponent.TYPE).graveId());
            grave.openUi(serverPlayerEntity, false, false);
        }
        return ActionResult.PASS;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.COMPASS;
    }


    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return null;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayerEntity player && !stack.isEmpty()) {
            if (stack.contains(GraveCompassComponent.TYPE)) {
                var compass = stack.get(GraveCompassComponent.TYPE);
                var grave = GraveManager.INSTANCE.getId(compass.graveId());

                if (grave == null) {
                    var count = stack.getCount();
                    stack.setCount(0);

                    if (compass.convertToVanilla()) {
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
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        var clientStack = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
        if (itemStack.contains(GraveCompassComponent.TYPE)) {
            var grave = GraveManager.INSTANCE.getId(itemStack.get(GraveCompassComponent.TYPE).graveId());
            if (grave != null) {
                clientStack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.of(grave.getLocation().asGlobalPos()), true));
            }
        } else {
            clientStack.set(DataComponentTypes.LODESTONE_TRACKER, new LodestoneTrackerComponent(Optional.empty(), true));
        }

        if (!clientStack.contains(DataComponentTypes.CUSTOM_NAME)) {
            if (
                    (clientStack.contains(DataComponentTypes.LODESTONE_TRACKER))
            ) {
                clientStack.set(DataComponentTypes.CUSTOM_NAME, Text.empty().append(itemStack.getItemName()).setStyle(Style.EMPTY.withItalic(false)));
            }
        }
        return clientStack;
    }
}
