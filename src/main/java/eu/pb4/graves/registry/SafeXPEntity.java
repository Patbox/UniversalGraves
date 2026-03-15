package eu.pb4.graves.registry;

import eu.pb4.graves.mixin.ExperienceOrbEntityAccessor;

import eu.pb4.graves.other.GraveUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SafeXPEntity extends ExperienceOrb implements PolymerEntity {
    public static EntityType<Entity> TYPE = FabricEntityTypeBuilder.create().entityFactory(SafeXPEntity::new).fireImmune().disableSummon().dimensions(EntityDimensions.fixed(0.5F, 0.5F)).trackRangeChunks(6).trackedUpdateRate(20).build(
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("universal_graves", "xp"))
    );
    public SafeXPEntity(Level world, double x, double y, double z, int amount) {
        this(TYPE, world);
        this.setPos(x, y, z);
        this.setYRot((float)(this.random.nextDouble() * 360.0D));
        this.setDeltaMovement((this.random.nextDouble() * 0.20000000298023224D - 0.10000000149011612D) * 2.0D, this.random.nextDouble() * 0.2D * 2.0D, (this.random.nextDouble() * 0.20000000298023224D - 0.10000000149011612D) * 2.0D);
        ((ExperienceOrbEntityAccessor) this).callSetValue(amount);
    }

    public SafeXPEntity(EntityType<Entity> entityType, Level world) {
        //noinspection unchecked
        super((EntityType<? extends ExperienceOrb>) (Object) entityType, world);
    }

    public static void award(ServerLevel world, Vec3 pos, int amount) {
        world.addFreshEntity(new SafeXPEntity(world, pos.x(), pos.y(), pos.z(), amount));
    }

    @Override
    public void playerTouch(Player player) {
        if (!this.level().isClientSide()) {
            // Clones vanilla logic to make sure other mods don't modify it
            if (player.takeXpDelay == 0) {
                player.takeXpDelay = 2;
                player.take(this, 1);
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
