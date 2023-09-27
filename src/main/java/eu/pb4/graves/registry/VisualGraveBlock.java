package eu.pb4.graves.registry;

import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.other.VisualGraveData;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings({"deprecation"})
public class VisualGraveBlock extends AbstractGraveBlock implements BlockEntityProvider {
    public static VisualGraveBlock INSTANCE = new VisualGraveBlock(Settings.copy(GraveBlock.INSTANCE).hardness(4).dropsNothing());

    public VisualGraveBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.ROTATION, IS_LOCKED, WATERLOGGED);
    }

    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.BLOCK;
    }

    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected @Nullable Grave getGraveData(World world, BlockPos pos) {
        return null;
    }

    @Override
    protected VisualGraveData getVisualData(World world, BlockPos pos, @Nullable Grave grave) {
        var entity = world.getBlockEntity(pos, VisualGraveBlockEntity.BLOCK_ENTITY_TYPE);
        return entity.isPresent() ? entity.get().getGrave() : VisualGraveData.DEFAULT;
    }

    @Override
    protected Map<String, Text> getPlaceholders(MinecraftServer server, VisualGraveData visualGrave, @Nullable Grave grave) {
        return visualGrave.getPlaceholders(server);
    }

    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new VisualGraveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return VisualGraveBlockEntity::tick;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {


        return this.getDefaultState().with(Properties.ROTATION, RotationPropertyHelper.fromYaw(ctx.getPlayerYaw() + 180.0F))
                .with(WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).isOf(Fluids.WATER));
    }
}
