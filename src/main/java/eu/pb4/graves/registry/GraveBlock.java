package eu.pb4.graves.registry;

import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.client.GravesModClient;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.GraveManager;
import eu.pb4.polymer.api.block.PlayerAwarePolymerBlock;
import eu.pb4.polymer.api.client.PolymerClientDecoded;
import eu.pb4.polymer.api.client.PolymerKeepModel;
import eu.pb4.polymer.api.utils.PolymerUtils;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"deprecation"})
public class GraveBlock extends Block implements PlayerAwarePolymerBlock, BlockEntityProvider, Waterloggable, PolymerClientDecoded, PolymerKeepModel {
    public static BooleanProperty IS_LOCKED = BooleanProperty.of("is_locked");

    public static GraveBlock INSTANCE = new GraveBlock();

    private GraveBlock() {
        super(AbstractBlock.Settings.of(Material.METAL).dropsNothing().dynamicBounds().strength(2, 999));
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

    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (world.getServer() == null) {
            return;
        }

        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof GraveBlockEntity grave && grave.getGrave() != null) {
                grave.getGrave().destroyGrave(world.getServer(), null);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity playerTemp) {
        if (!(playerTemp instanceof ServerPlayerEntity player)) {
            return;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof GraveBlockEntity graveBlockEntity && graveBlockEntity.getGrave() != null && graveBlockEntity.getGrave().canTakeFrom(player)) {
            try {
                var grave = graveBlockEntity.getGrave();
                grave.destroyGrave(player.getServer(), player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity playerTemp, Hand hand, BlockHitResult hit) {
        if (!(playerTemp instanceof ServerPlayerEntity player) || hand == Hand.OFF_HAND) {
            return playerTemp.isSneaking() ? ActionResult.PASS : ActionResult.FAIL;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof GraveBlockEntity graveBlockEntity && graveBlockEntity.getGrave() != null && graveBlockEntity.getGrave().canTakeFrom(player)) {
            try {
                var grave = graveBlockEntity.getGrave();

                grave.updateSelf(world.getServer());

                if (!grave.isRemoved()) {
                    if (ConfigManager.getConfig().configData.shiftClickTakesItems && player.isSneaking()) {
                        grave.quickEquip(player);
                    } else {
                        grave.openUi(player, true);
                    }
                }
                return ActionResult.SUCCESS;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return playerTemp.isSneaking() ? ActionResult.PASS : ActionResult.SUCCESS;
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
            .converter.getBlockState(state.get(Properties.ROTATION), state.get(IS_LOCKED), state.get(Properties.WATERLOGGED));
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
        var grave = GraveManager.INSTANCE.getByLocation(player.world, pos);

        if (grave != null) {
            boolean locked = state.get(IS_LOCKED);
            if (!GraveNetworking.sendGrave(player.networkHandler, pos, locked, grave.toVisualGraveData(), grave.getPlaceholders(player.server), null)) {
                ConfigManager.getConfig().style.converter.sendNbt(player, state, pos.toImmutable(), state.get(Properties.ROTATION), state.get(IS_LOCKED), grave.toVisualGraveData(), grave, null);
            }
        }
    }

    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return GraveBlockEntity::tick;
    }

    @Override
    public boolean shouldDecodePolymer() {
        return PolymerUtils.isOnClientSide() ? GravesModClient.config.enabled() : true;
    }
}
