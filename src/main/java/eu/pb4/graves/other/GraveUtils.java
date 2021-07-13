package eu.pb4.graves.other;


import eu.pb4.graves.event.GraveValidPosCheckEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GraveUtils {
    public static final Identifier REPLACEABLE_TAG = new Identifier("universal_graves","replaceable");

    public static BlockCheckResult findGravePosition(ServerPlayerEntity player, ServerWorld world, BlockPos blockPos, Tag<Block> replaceable) {
        int maxDistance = 8;
        int line = 1;

        blockPos = new BlockPos(blockPos.getX(), MathHelper.clamp(blockPos.getY(), world.getBottomY(), world.getTopY() - 1), blockPos.getZ());
        BlockResult result = isValidPos(player, world, blockPos, replaceable);
        BlockResult tempResult;
        if (result.allow) {
            return new BlockCheckResult(blockPos, result);
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

                            tempResult = isValidPos(player, world, pos, replaceable);
                            if (tempResult.priority >= result.priority) {
                                result = tempResult;
                            }
                            if (result.canCreate()) {
                                return new BlockCheckResult(pos.toImmutable(), result);
                            }
                        }
                    }
                }
                line++;
            }
            return new BlockCheckResult(null, result);
        }
    }


    private static BlockResult isValidPos(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Tag<Block> replaceable) {
        BlockState state = world.getBlockState(pos);
        if (pos.getY() >= world.getBottomY() && pos.getY() < world.getTopY() && !state.hasBlockEntity() && (state.isAir() || replaceable.contains(state.getBlock()))) {
            return GraveValidPosCheckEvent.EVENT.invoker().isValid(player, world, pos);
        } else {
            return BlockResult.BLOCK;
        }
    }

    public enum BlockResult {
        ALLOW(true, 2),
        BLOCK(false, 0),
        BLOCK_CLAIM(false, 1);

        private final boolean allow;
        private final int priority;

        BlockResult(boolean allow, int priority) {
            this.allow = allow;
            this.priority = priority;
        }

        public boolean canCreate() {
            return this.allow;
        }
    }

    public static record BlockCheckResult(@Nullable BlockPos pos, BlockResult result) {}


    public static String toWorldName(Identifier identifier) {
        List<String> parts = new ArrayList<>();
        {
            String[] words = identifier.getPath().split("_");
            for (String word : words) {
                String[] s = word.split("", 2);
                s[0] = s[0].toUpperCase(Locale.ROOT);
                parts.add(String.join("", s));
            }
        }
        return String.join("", parts);
    }
}
