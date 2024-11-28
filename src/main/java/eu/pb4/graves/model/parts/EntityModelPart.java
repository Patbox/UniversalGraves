package eu.pb4.graves.model.parts;

import com.google.common.collect.Iterables;
import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import eu.pb4.graves.GravesMod;
import eu.pb4.graves.mixin.LivingEntityAccessor;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.elements.EntityElement;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class EntityModelPart extends ModelPart<EntityElement<?>, EntityModelPart> {
    @SerializedName("entity_type")
    public EntityType<?> entityType;

    @SerializedName("entity_nbt")
    @Nullable
    public NbtCompound nbtCompound;

    @SerializedName("entity_pose")
    @Nullable
    public EntityPose entityPose;


    @Override
    public EntityElement<?> construct(ServerWorld world) {
        var entity = entityType == EntityType.PLAYER ? createPlayer(world) : entityType.create(world, SpawnReason.COMMAND);

        if (nbtCompound != null) {
            entity.readNbt(this.nbtCompound);
        }

        if (entityPose != null) {
            entity.setPose(this.entityPose);
        }
        var base = entityType == EntityType.PLAYER ? new PlayerElement((PlayerEntity) entity, world) : new EntityElement<>(entity, world);
        base.setOffset(this.position);
        return base;
    }

    private PlayerEntity createPlayer(World world) {
        return new PlayerEntity(world, BlockPos.ORIGIN, 0, new GameProfile(UUID.randomUUID(), "")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
    }

    @Override
    public ModelPartType type() {
        return ModelPartType.ENTITY;
    }

    public static class PlayerElement extends EntityElement<PlayerEntity> {
        private final int extraId = VirtualEntityUtils.requestEntityId();
        private final UUID extraUuid = UUID.randomUUID();
        private GameProfile profile;

        public PlayerElement(PlayerEntity entity, ServerWorld world) {
            super(entity, world);
            this.profile = new GameProfile(entity.getUuid(), "");
        }

        @Override
        public IntList getEntityIds() {
            return IntList.of(this.extraId, this.entity().getId());
        }

        @Override
        public void startWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
            var entry = new PlayerListS2CPacket.Entry(this.entity().getUuid(), this.profile, false, -1, GameMode.SURVIVAL, null, false, 0, null);
            {
                var packet = PolymerEntityUtils.createMutablePlayerListPacket(EnumSet.of(PlayerListS2CPacket.Action.ADD_PLAYER, PlayerListS2CPacket.Action.UPDATE_GAME_MODE, PlayerListS2CPacket.Action.UPDATE_LISTED));
                packet.getEntries().add(entry);
                packetConsumer.accept(packet);
            }
            super.startWatching(player, packetConsumer);

            packetConsumer.accept(new EntitySpawnS2CPacket(extraId, extraUuid,
                    this.entity().getX(), this.entity().getY(), this.entity().getZ(), 0, 0, EntityType.ITEM_DISPLAY, 0, Vec3d.ZERO, 0));
            packetConsumer.accept(VirtualEntityUtils.createRidePacket(this.entity().getId(), new int[] { this.extraId }));
        }

        @Override
        public void stopWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
            super.stopWatching(player, packetConsumer);
            packetConsumer.accept(new PlayerRemoveS2CPacket(List.of(this.entity().getUuid())));
        }

        public void copyTexture(GameProfile profile) {
            var texture = Iterables.getFirst(profile.getProperties().get("textures"), null);
            if (texture != null && texture.hasSignature()) {
                this.profile.getProperties().put("textures", texture);
            } else {
                this.profile.getProperties().removeAll("textures");
            }
        }
    }
}
