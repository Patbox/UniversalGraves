package eu.pb4.graves.compat;

import eu.pb4.graves.GravesApi;
import eu.pb4.graves.grave.GraveInventoryMask;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.api.events.OnDropCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record AccessoriesCompat() implements GraveInventoryMask {
    private static final String TYPE_TAG = "Type";
    private static final String SLOT_TAG = "Slot";

    public static void register() {
        GravesApi.registerInventoryMask(Identifier.of("universal_graves", "accessories"), new AccessoriesCompat());
    }

    @Override
    public void addToGrave(ServerPlayerEntity player, ItemConsumer consumer) {
        var cap = AccessoriesCapability.get(player);
        if (cap == null) {
            return;
        }

        cap.getContainers().forEach((s, accessoriesContainer) -> {
            var defRule = accessoriesContainer.slotType() != null ? Objects.requireNonNull(accessoriesContainer.slotType()).dropRule() : DropRule.DEFAULT;
            addToGrave(player, consumer, s, accessoriesContainer.getAccessories(), "", defRule);
            addToGrave(player, consumer, s, accessoriesContainer.getCosmeticAccessories(), "cosmetic", defRule);
        });
    }

    private void addToGrave(ServerPlayerEntity player, ItemConsumer consumer, String slotName, ExpandedSimpleContainer accessories, String type, DropRule defaultDropRule) {
        var dmg = player.getRecentDamageSource();
        if (dmg == null) {
            dmg = player.getDamageSources().generic();
        }
        for (var i = 0; i < accessories.size(); i++) {
            var stack = accessories.getStack(i);
            if (stack.isEmpty() || !GravesApi.canAddItem(player, stack)) {
                return;
            }

            var ref = SlotReference.of(player, slotName, i);

            var dropRule = AccessoriesAPI.getOrDefaultAccessory(stack).getDropRule(stack, ref, dmg);

            dropRule = OnDropCallback.EVENT.invoker().onDrop(dropRule, stack, ref, dmg);

            if (dropRule == DropRule.DEFAULT) {
                dropRule = defaultDropRule;
            }

            if (dropRule == DropRule.DEFAULT) {
                if (EnchantmentHelper.hasAnyEnchantmentsWith(stack, EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP)) {
                    dropRule = DropRule.DESTROY;
                } else {
                    dropRule = DropRule.DROP;
                }
            }

            if (dropRule == DropRule.DROP) {
                var nbt = new NbtCompound();
                nbt.putString(TYPE_TAG, TYPE_TAG);
                nbt.putString(SLOT_TAG, slotName);

                consumer.addItem(stack.copy(), i, nbt);
                accessories.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public boolean moveToPlayerExactly(ServerPlayerEntity player, ItemStack stack, int slot, NbtElement extraData) {
        var typeId = ((NbtCompound) extraData).getString(TYPE_TAG);
        var slotId = ((NbtCompound) extraData).getString(SLOT_TAG);

        var inventory = getInventory(player, typeId, slotId);

        if (inventory != null) {
            if (inventory.getStack(slot).isEmpty()) {
                inventory.setStack(slot, stack.copyAndEmpty());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean moveToPlayerClosest(ServerPlayerEntity player, ItemStack stack, int slot, NbtElement data) {
        var type = ((NbtCompound) data).getString(TYPE_TAG);
        var slotId = ((NbtCompound) data).getString(SLOT_TAG);

        var inventory = getInventory(player, type, slotId);

        if (inventory != null) {
            int size = inventory.size();

            for (int i = 0; i < size; i++) {
                if (inventory.getStack(i).isEmpty()) {
                    inventory.setStack(i, stack.copyAndEmpty());
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private ExpandedSimpleContainer getInventory(ServerPlayerEntity player, String type, String slotId) {
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
