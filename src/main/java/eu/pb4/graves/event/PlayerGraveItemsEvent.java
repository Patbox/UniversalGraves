package eu.pb4.graves.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public interface PlayerGraveItemsEvent {
    Event<PlayerGraveItemsEvent> EVENT = EventFactory.createArrayBacked(PlayerGraveItemsEvent.class,
                (listeners) -> (player, items) -> {
                    for (PlayerGraveItemsEvent listener : listeners) {
                        listener.modifyItems(player, items);
                    }
                });

    void modifyItems(ServerPlayerEntity player, List<ItemStack> items);
}
