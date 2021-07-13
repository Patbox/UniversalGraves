package eu.pb4.graves.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface PlayerGraveCreationEvent {
    Event<PlayerGraveCreationEvent> EVENT = EventFactory.createArrayBacked(PlayerGraveCreationEvent.class,
                (listeners) -> (player) -> {
                    CreationResult result = CreationResult.ALLOW;
                    for (PlayerGraveCreationEvent listener : listeners) {
                         result = listener.shouldCreate(player);
                         if (!result.allow) {
                             return result;
                         }
                    }
                    return result;
                });

    CreationResult shouldCreate(ServerPlayerEntity player);

    enum CreationResult {
        ALLOW(true),
        BLOCK(false),
        BLOCK_PVP(false),
        BLOCK_CLAIM(false),
        BLOCK_SILENT(false);

        private final boolean allow;

        CreationResult(boolean allow) {
            this.allow = allow;
        }

        public boolean canCreate() {
            return this.allow;
        }
    }
}
