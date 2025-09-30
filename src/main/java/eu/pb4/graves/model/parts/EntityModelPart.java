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
import net.minecraft.storage.NbtReadView;
import net.minecraft.util.ErrorReporter;
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
        if (entityType == EntityType.PLAYER) {
            entityType = EntityType.MANNEQUIN;
        }

        var entity = entityType.create(world, SpawnReason.COMMAND);

        if (nbtCompound != null) {
            entity.readData(NbtReadView.create(ErrorReporter.EMPTY, world.getRegistryManager(), this.nbtCompound));
        }

        if (entityPose != null) {
            entity.setPose(this.entityPose);
        }
        var base = new EntityElement<>(entity, world);
        base.setOffset(this.position);
        return base;
    }

    @Override
    public ModelPartType type() {
        return ModelPartType.ENTITY;
    }
}
