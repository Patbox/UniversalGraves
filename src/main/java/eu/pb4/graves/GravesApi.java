package eu.pb4.graves;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.event.GraveValidPosCheckEvent;
import eu.pb4.graves.event.PlayerGraveCreationEvent;
import eu.pb4.graves.event.PlayerGraveItemAddedEvent;
import eu.pb4.graves.grave.*;
import eu.pb4.graves.other.GraveUtils;
import eu.pb4.graves.other.Location;
import eu.pb4.graves.other.VanillaInventoryMask;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class GravesApi {
    private static final BiMap<Identifier, GraveInventoryMask> MASKS_MAP = HashBiMap.create();
    private static final List<GraveInventoryMask> MASKS = new ArrayList<>();

    public static Event<PlayerGraveCreationEvent> CREATION_EVENT = PlayerGraveCreationEvent.EVENT;
    public static Event<GraveValidPosCheckEvent> VALID_POS_CHECK_EVENT = GraveValidPosCheckEvent.EVENT;
    public static Event<PlayerGraveItemAddedEvent> ADD_ITEM_EVENT = PlayerGraveItemAddedEvent.EVENT;

    public static Collection<Grave> getGravesOf(UUID uuid) {
        return ImmutableList.copyOf(GraveManager.INSTANCE.getByUuid(uuid));
    }

    public static Optional<Grave> getGraveAt(Identifier world, BlockPos pos) {
        return Optional.ofNullable(GraveManager.INSTANCE.getByLocation(new Location(world, pos)));
    }

    public static Collection<Grave> getAllGraves() {
        return ImmutableList.copyOf(GraveManager.INSTANCE.getAll());
    }

    public static boolean canAddItem(ServerPlayerEntity player, ItemStack itemStack) {
        return !itemStack.isEmpty()
                && PlayerGraveItemAddedEvent.EVENT.invoker().canAddItem(player, itemStack) != ActionResult.FAIL
                && !GraveUtils.hasSkippedEnchantment(itemStack)
                && !EnchantmentHelper.hasVanishingCurse(itemStack);
    }

    @Nullable
    public static Config getConfig() {
        return ConfigManager.getConfig();
    }

    public static Identifier getInventoryMaskId(GraveInventoryMask inventoryMask) {
        return MASKS_MAP.inverse().get(inventoryMask);
    }

    public static GraveInventoryMask getInventoryMask(Identifier identifier) {
        return MASKS_MAP.get(identifier);
    }

    public static void registerInventoryMask(Identifier identifier, GraveInventoryMask mask) {
        if (!MASKS_MAP.containsKey(identifier)) {
            MASKS_MAP.put(identifier, mask);
            MASKS.add(mask);
        } else {
            throw new RuntimeException("You can't register same mask id twice!");
        }
    }

    public static Collection<GraveInventoryMask> getAllInventoryMasks() {
        return MASKS;
    }

    public static GraveInventoryMask getDefaultedInventoryMask(Identifier identifier) {
        var mask = getInventoryMask(identifier);
        return mask != null ? mask : VanillaInventoryMask.INSTANCE;
    }

    public static void removeInventoryMask(GraveInventoryMask mask) {
        MASKS.remove(mask);
        MASKS_MAP.inverse().remove(mask);
    }

    public static void removeInventoryMask(Identifier mask) {
        MASKS.remove(MASKS_MAP.remove(mask));
    }
}
