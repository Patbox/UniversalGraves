package eu.pb4.graves;

import com.google.common.collect.ImmutableList;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.event.GraveValidPosCheckEvent;
import eu.pb4.graves.event.PlayerGraveCreationEvent;
import eu.pb4.graves.event.PlayerGraveItemAddedEvent;
import eu.pb4.graves.event.PlayerGraveItemsEvent;
import eu.pb4.graves.grave.GraveBlockEntity;
import eu.pb4.graves.grave.GraveInfo;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.graves.grave.GravesLookType;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class GravesApi {
    public static Event<PlayerGraveCreationEvent> CREATION_EVENT = PlayerGraveCreationEvent.EVENT;
    public static Event<GraveValidPosCheckEvent> VALID_POS_CHECK_EVENT = GraveValidPosCheckEvent.EVENT;
    public static Event<PlayerGraveItemAddedEvent> ADD_ITEM_EVENT = PlayerGraveItemAddedEvent.EVENT;
    public static Event<PlayerGraveItemsEvent> MODIFY_ITEMS_EVENT = PlayerGraveItemsEvent.EVENT;

    public static Collection<GraveInfo> getGravesOf(UUID uuid) {
        return ImmutableList.copyOf(GraveManager.INSTANCE.getByUuid(uuid));
    }

    public static Optional<GraveInfo> getGraveInfoAt(BlockPos pos) {
        return Optional.ofNullable(GraveManager.INSTANCE.getByPos(pos));
    }

    public static Optional<GraveBlockEntity> getGraveAt(ServerWorld world, BlockPos pos) {
        return world.getBlockEntity(pos, GraveBlockEntity.BLOCK_ENTITY_TYPE);
    }

    public static Collection<GraveInfo> getAllGraves() {
        return ImmutableList.copyOf(GraveManager.INSTANCE.getAll());
    }

    public static void createStyle(Identifier identifier, GravesLookType.Converter converter) {
        GravesLookType.create(identifier, converter);
    }

    @Nullable
    public static Config getConfig() {
        return ConfigManager.getConfig();
    }
}
