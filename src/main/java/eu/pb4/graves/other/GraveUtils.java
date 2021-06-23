package eu.pb4.graves.other;


import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;

public class GraveUtils {
    public static final Identifier REPLACEABLE_TAG = new Identifier("universal_graves","replaceable");

    public static BlockPos findGravePosition(ServerWorld world, BlockPos blockPos, Tag<Block> replaceable) {
        int maxDistance = 5;
        int line = 1;

        blockPos = new BlockPos(blockPos.getX(), MathHelper.clamp(blockPos.getY(), world.getBottomY(), world.getTopY() - 1), blockPos.getZ());
        if (isValidPos(world, blockPos, replaceable)) {
            return blockPos;
        } else {
            BlockPos.Mutable pos = new BlockPos.Mutable(blockPos.getX(), blockPos.getY(), blockPos.getZ());

            while (line <= maxDistance) {
                int side = line * 2 + 1;
                for (int oY = 0; oY < side; oY++) {
                    for (int oX = 0; oX < side; oX++) {
                        for (int oZ = 0; oZ < side; oZ++) {
                            pos.set(blockPos.getX() - line + oX, blockPos.getY() - line + oY, blockPos.getZ() - line + oZ);

                            if ((oX > 0 && oX < side - 1) && (oY > 0 && oY < side - 1) && (oZ > 0 && oZ < side - 1)) {
                                continue;
                            }
                            if (isValidPos(world, pos, replaceable)) {
                                return pos.toImmutable();
                            }
                        }
                    }
                }
                line++;
            }
            return null;
        }
    }


    private static boolean isValidPos(ServerWorld world, BlockPos pos, Tag<Block> replaceable) {
        BlockState state = world.getBlockState(pos);
        return pos.getY() >= world.getBottomY() && pos.getY() < world.getTopY() && !state.hasBlockEntity() && (state.isAir() || replaceable.contains(state.getBlock()));
    }
}
