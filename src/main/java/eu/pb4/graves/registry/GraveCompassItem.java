package eu.pb4.graves.registry;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.other.PlayerAdditions;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class GraveCompassItem extends Item implements PolymerItem {
    public GraveCompassItem(Properties settings) {
        super(settings.stacksTo(1));
    }

    public static ItemStack create(long graveId, boolean toVanilla) {
        var stack = new ItemStack(GravesRegistry.GRAVE_COMPASS_ITEM);
        stack.set(GraveCompassComponent.TYPE, new GraveCompassComponent(graveId, toVanilla));
        return stack;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (user instanceof ServerPlayer serverPlayerEntity && ConfigManager.getConfig().interactions.useDeathCompassToOpenGui && stack.has(GraveCompassComponent.TYPE)) {
            Grave grave = GraveManager.INSTANCE.getId(stack.get(GraveCompassComponent.TYPE).graveId());
            grave.openUi(serverPlayerEntity, false, false);
        }
        return InteractionResult.PASS;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.COMPASS;
    }


    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        return null;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (entity instanceof ServerPlayer player && !stack.isEmpty()) {
            if (stack.has(GraveCompassComponent.TYPE)) {
                var compass = stack.get(GraveCompassComponent.TYPE);
                var grave = GraveManager.INSTANCE.getId(compass.graveId());

                if (grave == null) {
                    var count = stack.getCount();
                    stack.setCount(0);

                    if (compass.convertToVanilla()) {
                        player.addItem(new ItemStack(Items.COMPASS, count));
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
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context, HolderLookup.Provider lookup) {
        var clientStack = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context, lookup);
        if (itemStack.has(GraveCompassComponent.TYPE)) {
            var grave = GraveManager.INSTANCE.getId(itemStack.get(GraveCompassComponent.TYPE).graveId());
            if (grave != null) {
                clientStack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(grave.getLocation().asGlobalPos()), true));
            }
        } else {
            clientStack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), true));
        }

        if (!clientStack.has(DataComponents.CUSTOM_NAME)) {
            if (
                    (clientStack.has(DataComponents.LODESTONE_TRACKER))
            ) {
                clientStack.set(DataComponents.CUSTOM_NAME, Component.empty().append(itemStack.getItemName()).setStyle(Style.EMPTY.withItalic(false)));
            }
        }
        return clientStack;
    }
}
