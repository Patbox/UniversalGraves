package eu.pb4.graves.registry;


import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.client.GravesModClient;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.polymer.api.block.PlayerAwarePolymerBlock;
import eu.pb4.polymer.api.client.PolymerClientDecoded;
import eu.pb4.polymer.api.client.PolymerKeepModel;
import eu.pb4.polymer.api.utils.PolymerUtils;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings({"deprecation"})
public abstract class AbstractGraveBlock extends Block implements PlayerAwarePolymerBlock, Waterloggable, PolymerClientDecoded, PolymerKeepModel {
    public static BooleanProperty IS_LOCKED = BooleanProperty.of("is_locked");

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

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (!PolymerUtils.isOnClientSide() && context instanceof EntityShapeContext entityShapeContext && entityShapeContext.getEntity() instanceof ServerPlayerEntity player) {
            return ConfigManager.getConfig().style.converter.getBlockState(
                    state.get(Properties.ROTATION),
                    state.get(IS_LOCKED),
                    state.get(Properties.WATERLOGGED),
                    player
            ).getCollisionShape(world, pos, context);
        }

        return super.getCollisionShape(state, world, pos, context);
    }

    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.BLOCK;
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return (PolymerUtils.isOnClientSide() ? GravesModClient.serverSideModel : ConfigManager.getConfig().style)
                .converter.getBlock(state.get(IS_LOCKED));
    }

    @Override
    public Block getPolymerBlock(ServerPlayerEntity player, BlockState state) {
        if (GraveNetworking.canReceive(player.networkHandler)) {
            return this;
        }
        return getPolymerBlock(state);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return (PolymerUtils.isOnClientSide() ? GravesModClient.serverSideModel : ConfigManager.getConfig().style)
                .converter.getBlockState(state.get(Properties.ROTATION), state.get(IS_LOCKED), state.get(Properties.WATERLOGGED), null);
    }

    @Override
    public BlockState getPolymerBlockState(ServerPlayerEntity player, BlockState state) {
        if (GraveNetworking.canReceive(player.networkHandler)) {
            return state;
        }
        return getPolymerBlockState(state);
    }

    @Override
    public void onPolymerBlockSend(ServerPlayerEntity player, BlockPos.Mutable pos, BlockState state) {
        var grave = getGraveData(player.world, pos);
        var visualGrave = getVisualData(player.world, pos, grave);
        var placeholders = this.getPlaceholders(player.server, visualGrave, grave);
        var overrides = this.getTextOverrides(player.world, pos);

        boolean locked = state.get(IS_LOCKED);

        if (!GraveNetworking.sendGrave(player.networkHandler, pos, locked, visualGrave, placeholders, overrides)) {
            ConfigManager.getConfig().style.converter.sendNbt(player, state, pos.toImmutable(), state.get(Properties.ROTATION), state.get(IS_LOCKED), visualGrave, grave, overrides);
        }
    }


    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Nullable
    protected abstract Grave getGraveData(World world, BlockPos pos);

    @Nullable
    protected abstract Text[] getTextOverrides(World world, BlockPos pos);

    protected abstract VisualGraveData getVisualData(World world, BlockPos pos, @Nullable Grave grave);

    protected abstract Map<String, Text> getPlaceholders(MinecraftServer server, VisualGraveData visualGrave, @Nullable Grave grave);

    @Override
    public boolean shouldDecodePolymer() {
        return !PolymerUtils.isOnClientSide() || GravesModClient.config.enabled();
    }
}
