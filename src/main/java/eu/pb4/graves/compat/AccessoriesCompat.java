package eu.pb4.graves.compat;

import eu.pb4.graves.GravesApi;
import eu.pb4.graves.grave.GraveInventoryMask;
import eu.pb4.graves.other.VanillaInventoryMask;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.events.OnDropCallback;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AccessoriesCompat implements GraveInventoryMask {
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
                if (EnchantmentHelper.hasVanishingCurse(stack)) {
                    dropRule = DropRule.DESTROY;
                } else {
                    dropRule = DropRule.DROP;
                }
            }

            if (dropRule == DropRule.DROP) {
                var nbt = new NbtCompound();
                nbt.putString(TYPE_TAG, type);
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

        if(slot == null) return null;

        return ("cosmetic".equals(type))
                ? slot.getCosmeticAccessories()
                : slot.getAccessories();
    }
}
