package eu.pb4.graves.model.parts;

import com.google.common.collect.Iterables;
import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import eu.pb4.graves.mixin.LivingEntityAccessor;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.elements.AbstractElement;
import eu.pb4.polymer.virtualentity.api.elements.EntityElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;

public class ParticleModelPart extends ModelPart<ParticleModelPart.ParticleElement, ParticleModelPart> {
    @SerializedName("particle")
    public ParticleEffect particleEffect;

    @SerializedName("wait_duration")
    public int waitDuration = 5;

    @SerializedName("delta")
    public Vector3f delta = new Vector3f();

    @SerializedName("speed")
    public float speed = 0;

    @SerializedName("count")
    public int count = 0;

    @Override
    public ParticleElement construct(ServerWorld world) {
        return new ParticleElement(this.particleEffect, this.delta, this.speed, this.count, this.waitDuration);
    }

    @Override
    public ModelPartType type() {
        return ModelPartType.PARTICLE;
    }

    public static class ParticleElement extends AbstractElement {
        private final ParticleEffect particleEffect;
        private final Vector3f delta;
        private final float speed;
        private final int count;
        private final int waitDuration;
        private int tick = 0;
        private Packet<ClientPlayPacketListener> packet;

        public ParticleElement(ParticleEffect particleEffect, Vector3f delta, float speed, int count, int waitDuration) {
            this.particleEffect = particleEffect;
            this.delta = delta;
            this.speed = speed;
            this.count = count;
            this.waitDuration = Math.max(waitDuration, 1);
        }

        @Override
        public void setOffset(Vec3d offset) {
            super.setOffset(offset);
            this.packet = null;
        }

        @Override
        public IntList getEntityIds() {
            return IntList.of();
        }

        @Override
        public void startWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {}

        @Override
        public void stopWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {}

        @Override
        public void notifyMove(Vec3d oldPos, Vec3d currentPos, Vec3d delta) {}

        @Override
        public void tick() {
            if (this.tick++ % this.waitDuration == 0) {
                if (this.packet == null) {
                    var pos = Objects.requireNonNull(this.getHolder()).getPos().add(this.getOffset());
                    this.packet = new ParticleS2CPacket(this.particleEffect, false, false, pos.x, pos.y, pos.z, this.delta.x, this.delta.y, this.delta.z, this.speed, this.count);
                }

                Objects.requireNonNull(this.getHolder()).sendPacket(this.packet);
            }

        }
    }
}
