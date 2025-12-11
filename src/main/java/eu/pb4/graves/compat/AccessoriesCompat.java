package eu.pb4.graves.compat;

import eu.pb4.graves.GravesApi;
import eu.pb4.graves.grave.GraveInventoryMask;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.events.DropRule;
import io.wispforest.accessories.api.events.OnDropCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.core.ExpandedContainer;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public record AccessoriesCompat() implements GraveInventoryMask {
    private static final String TYPE_TAG = "Type";
    private static final String SLOT_TAG = "Slot";

    public static void register() {
        GravesApi.registerInventoryMask(Identifier.fromNamespaceAndPath("universal_graves", "accessories"), new AccessoriesCompat());
    }

    @Override
    public void addToGrave(ServerPlayer player, ItemConsumer consumer) {
        var cap = AccessoriesCapability.get(player);
        if (cap == null) {
            return;
        }

        cap.getContainers().forEach((s, accessoriesContainer) -> {
            addToGrave(player, consumer, s, accessoriesContainer.getAccessories(), "", DropRule.DEFAULT);
            addToGrave(player, consumer, s, accessoriesContainer.getCosmeticAccessories(), "cosmetic", DropRule.DEFAULT);
        });
    }

    private void addToGrave(ServerPlayer player, ItemConsumer consumer, String slotName, ExpandedContainer accessories, String type, DropRule defaultDropRule) {
        var dmg = player.getLastDamageSource();
        if (dmg == null) {
            dmg = player.damageSources().generic();
        }
        for (var i = 0; i < accessories.getContainerSize(); i++) {
            var stack = accessories.getItem(i);
            if (stack.isEmpty() || !GravesApi.canAddItem(player, stack)) {
                continue;
            }

            var ref = SlotReference.of(player, slotName, i);

            var dropRule = AccessoryRegistry.getAccessoryOrDefault(stack).getDropRule(stack, ref, dmg);

            dropRule = OnDropCallback.EVENT.invoker().onDrop(dropRule, stack, ref, dmg);

            if (dropRule == DropRule.DEFAULT) {
                dropRule = defaultDropRule;
            }

            if (dropRule == DropRule.DEFAULT) {
                if (EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                    dropRule = DropRule.DESTROY;
                } else {
                    dropRule = DropRule.DROP;
                }
            }

            if (dropRule == DropRule.DROP) {
                var nbt = new CompoundTag();
                nbt.putString(TYPE_TAG, type);
                nbt.putString(SLOT_TAG, slotName);

                consumer.addItem(stack.copy(), i, nbt);
                accessories.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public boolean moveToPlayerExactly(ServerPlayer player, ItemStack stack, int slot, Tag extraData) {
        var typeId = ((CompoundTag) extraData).getStringOr(TYPE_TAG, "");
        var slotId = ((CompoundTag) extraData).getStringOr(SLOT_TAG, "");

        var inventory = getInventory(player, typeId, slotId);

        if (inventory != null) {
            if (inventory.getItem(slot).isEmpty()) {
                inventory.setItem(slot, stack.copyAndClear());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean moveToPlayerClosest(ServerPlayer player, ItemStack stack, int slot, Tag data) {
        var type = ((CompoundTag) data).getStringOr(TYPE_TAG, "");
        var slotId = ((CompoundTag) data).getStringOr(SLOT_TAG, "");

        var inventory = getInventory(player, type, slotId);

        if (inventory != null) {
            int size = inventory.getContainerSize();

            for (int i = 0; i < size; i++) {
                if (inventory.getItem(i).isEmpty()) {
                    inventory.setItem(i, stack.copyAndClear());
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private ExpandedContainer getInventory(ServerPlayer player, String type, String slotId) {
        var cap = AccessoriesCapability.get(player);
        if (cap == null) {
            return null;
        }

        var slot = cap.getContainers().get(slotId);

        return slot != null ? switch (type) {
            case "cosmetic" -> slot.getCosmeticAccessories();
            case null, default -> slot.getAccessories();
        } : null;
    }
}
