package eu.pb4.graves.registry;


import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.model.GraveModelHandler;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings({"deprecation"})
public abstract class AbstractGraveBlock extends Block implements PolymerBlock, Waterloggable, BlockWithElementHolder {
    public static BooleanProperty IS_LOCKED = BooleanProperty.of("is_locked");
    public static IntProperty ROTATION = Properties.ROTATION;

    protected AbstractGraveBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(Properties.WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.ROTATION, IS_LOCKED, Properties.WATERLOGGED);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return false;
    }

    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.BLOCK;
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.DIRT;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.BARRIER.getDefaultState();
    }

    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new GraveModelHandler(initialBlockState);
    }

    @Nullable
    protected abstract Grave getGraveData(World world, BlockPos pos);

    @Nullable
    protected abstract Text[] getTextOverrides(World world, BlockPos pos);

    protected abstract VisualGraveData getVisualData(World world, BlockPos pos, @Nullable Grave grave);

    protected abstract Map<String, Text> getPlaceholders(MinecraftServer server, VisualGraveData visualGrave, @Nullable Grave grave);
}
