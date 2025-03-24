package eu.pb4.graves.registry;

import eu.pb4.graves.mixin.ExperienceOrbEntityAccessor;

import eu.pb4.graves.other.GraveUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

public class SafeXPEntity extends ExperienceOrbEntity implements PolymerEntity {
    public static EntityType<Entity> TYPE = FabricEntityTypeBuilder.create().entityFactory(SafeXPEntity::new).fireImmune().disableSummon().dimensions(EntityDimensions.fixed(0.5F, 0.5F)).trackRangeChunks(6).trackedUpdateRate(20).build(
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("universal_graves", "xp"))
    );
    public SafeXPEntity(World world, double x, double y, double z, int amount) {
        this(TYPE, world);
        this.setPosition(x, y, z);
        this.setYaw((float)(this.random.nextDouble() * 360.0D));
        this.setVelocity((this.random.nextDouble() * 0.20000000298023224D - 0.10000000149011612D) * 2.0D, this.random.nextDouble() * 0.2D * 2.0D, (this.random.nextDouble() * 0.20000000298023224D - 0.10000000149011612D) * 2.0D);
        ((ExperienceOrbEntityAccessor) this).callSetValue(amount);
    }

    public SafeXPEntity(EntityType<Entity> entityType, World world) {
        //noinspection unchecked
        super((EntityType<? extends ExperienceOrbEntity>) (Object) entityType, world);
    }

    public static void spawn(ServerWorld world, Vec3d pos, int amount) {
        world.spawnEntity(new SafeXPEntity(world, pos.getX(), pos.getY(), pos.getZ(), amount));
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (!this.getWorld().isClient) {
            // Clones vanilla logic to make sure other mods don't modify it
            if (player.experiencePickUpDelay == 0) {
                player.experiencePickUpDelay = 2;
                player.sendPickup(this, 1);
                GraveUtils.grandExperience(player, this.getValue());

                this.discard();
            }
        }
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityType.EXPERIENCE_ORB;
    }
}
