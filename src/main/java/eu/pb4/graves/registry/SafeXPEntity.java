package eu.pb4.graves.registry;

import eu.pb4.graves.mixin.ExperienceOrbEntityAccessor;
import eu.pb4.polymer.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SafeXPEntity extends ExperienceOrbEntity implements PolymerEntity {
    public static EntityType<Entity> TYPE = FabricEntityTypeBuilder.create().entityFactory(SafeXPEntity::new).fireImmune().disableSummon().dimensions(EntityDimensions.fixed(0.5F, 0.5F)).trackRangeChunks(6).trackedUpdateRate(20).build();
    public SafeXPEntity(World world, double x, double y, double z, int amount) {
        this(TYPE, world);
        this.setPosition(x, y, z);
        this.setYaw((float)(this.random.nextDouble() * 360.0D));
        this.setVelocity((this.random.nextDouble() * 0.20000000298023224D - 0.10000000149011612D) * 2.0D, this.random.nextDouble() * 0.2D * 2.0D, (this.random.nextDouble() * 0.20000000298023224D - 0.10000000149011612D) * 2.0D);
        ((ExperienceOrbEntityAccessor) this).setAmount(amount);
    }

    public SafeXPEntity(EntityType<Entity> entityType, World world) {
        super((EntityType<? extends ExperienceOrbEntity>) (Object) entityType, world);
    }

    public static void spawn(ServerWorld world, Vec3d pos, int amount) {
        world.spawnEntity(new SafeXPEntity(world, pos.getX(), pos.getY(), pos.getZ(), amount));
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (!this.world.isClient) {
            // Clones vanilla logic to make sure other mods don't modify it
            if (player.experiencePickUpDelay == 0) {
                player.experiencePickUpDelay = 2;
                player.sendPickup(this, 1);
                var experience = this.getExperienceAmount();

                player.addScore(experience);
                player.experienceProgress += (float)experience / (float)player.getNextLevelExperience();
                player.totalExperience = MathHelper.clamp(player.totalExperience + experience, 0, 2147483647);

                while(player.experienceProgress < 0.0F) {
                    float f = player.experienceProgress * (float)player.getNextLevelExperience();
                    if (player.experienceLevel > 0) {
                        player.addExperienceLevels(-1);
                        player.experienceProgress = 1.0F + f / (float)player.getNextLevelExperience();
                    } else {
                        player.addExperienceLevels(-1);
                        player.experienceProgress = 0.0F;
                    }
                }

                while(player.experienceProgress >= 1.0F) {
                    player.experienceProgress = (player.experienceProgress - 1.0F) * (float)player.getNextLevelExperience();
                    player.addExperienceLevels(1);
                    player.experienceProgress /= (float)player.getNextLevelExperience();
                }

                this.discard();
            }
        }
    }

    @Override
    public EntityType<?> getPolymerEntityType() {
        return EntityType.EXPERIENCE_BOTTLE;
    }
}
