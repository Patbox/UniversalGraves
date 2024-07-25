package eu.pb4.graves.event;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.other.GraveUtils;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * This even is use for checking, if graves is in valid position
 */
public interface GraveValidPosCheckEvent {
    Event<GraveValidPosCheckEvent> EVENT = EventFactory.createArrayBacked(GraveValidPosCheckEvent.class,
                (listeners) -> (player, world, pos) -> {
                    GraveUtils.BlockResult result = GraveUtils.BlockResult.ALLOW;
                    for (GraveValidPosCheckEvent listener : listeners) {
                         result = listener.isValid(player, world, pos);
                         if (!result.canCreate()) {
                             return result;
                         }
                    }
                    return result;
                });

    GraveUtils.BlockResult isValid(GameProfile player, ServerWorld world, BlockPos pos);
}
