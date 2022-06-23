package eu.pb4.graves.other;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.ProtectionProvider;
import eu.pb4.graves.grave.GraveHolder;
import eu.pb4.graves.registry.GraveBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class GraveProtectionProvider implements ProtectionProvider {
    public static final ProtectionProvider INSTANCE = new GraveProtectionProvider();

    private GraveProtectionProvider() {}

    @Override
    public boolean isProtected(World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof GraveBlockEntity;
    }

    @Override
    public boolean isAreaProtected(World world, Box area) {
        return false;
    }

    @Override
    public boolean canBreakBlock(World world, BlockPos pos, GameProfile profile, @Nullable PlayerEntity player) {
        var be = world.getBlockEntity(pos, GraveBlockEntity.BLOCK_ENTITY_TYPE);

        return be.isEmpty() || (be.get().getGrave() != null && be.get().getGrave().canTakeFrom(profile));
    }

    @Override
    public boolean canDamageEntity(World world, Entity entity, GameProfile profile, @Nullable PlayerEntity player) {
        return !(entity instanceof GraveHolder graveHolder && graveHolder.getGrave() != null && !graveHolder.getGrave().canTakeFrom(profile));
    }
}
