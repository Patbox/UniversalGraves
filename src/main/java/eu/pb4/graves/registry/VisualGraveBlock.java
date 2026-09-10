package eu.pb4.graves.registry;

import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.other.VisualGraveData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings({"deprecation"})
public class VisualGraveBlock extends AbstractGraveBlock implements EntityBlock {

    public VisualGraveBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.ROTATION_16, IS_LOCKED, WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected @Nullable Grave getGraveData(Level world, BlockPos pos) {
        return null;
    }

    @Override
    protected VisualGraveData getVisualData(Level world, BlockPos pos, @Nullable Grave grave) {
        var entity = world.getBlockEntity(pos, VisualGraveBlockEntity.BLOCK_ENTITY_TYPE);
        return entity.isPresent() ? entity.get().getGrave() : VisualGraveData.DEFAULT;
    }

    @Override
    protected Map<String, Component> getPlaceholders(MinecraftServer server, VisualGraveData visualGrave, @Nullable Grave grave) {
        return visualGrave.getPlaceholders(server);
    }

    public boolean canPathfindThrough(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VisualGraveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return VisualGraveBlockEntity::tick;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {


        return this.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, RotationSegment.convertToSegment(ctx.getRotation() + 180.0F))
                .setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
    }
}
