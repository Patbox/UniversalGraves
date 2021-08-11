package eu.pb4.graves.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

import java.util.List;

public interface PlayerGraveItemAddedEvent {
    Event<PlayerGraveItemAddedEvent> EVENT = EventFactory.createArrayBacked(PlayerGraveItemAddedEvent.class,
                (listeners) -> (player, item) -> {
                    for (PlayerGraveItemAddedEvent listener : listeners) {
                        ActionResult result = listener.canAddItem(player, item);

                        if (result != ActionResult.PASS) {
                            return result;
                        }
                    }
                    return ActionResult.PASS;
                });

    ActionResult canAddItem(ServerPlayerEntity player, ItemStack item);
}
