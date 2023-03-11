package eu.pb4.graves.registry;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.other.VisualGraveData;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings({"deprecation"})
public class GraveBlock extends AbstractGraveBlock implements BlockEntityProvider {
    public static GraveBlock INSTANCE = new GraveBlock();

    private GraveBlock() {
        super(AbstractBlock.Settings.of(Material.METAL).dropsNothing().dynamicBounds().strength(2, 999));
        this.setDefaultState(this.getStateManager().getDefaultState().with(Properties.WATERLOGGED, false));
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
                if (ConfigManager.getConfig().configData.restoreBlockAfterPlayerBreaking) {
                    graveBlockEntity.breakBlock();
                }
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
                    if (ConfigManager.getConfig().configData.shiftClickTakesItems && (player.isSneaking() || !ConfigManager.getConfig().configData.clickGraveToOpenGui)) {
                        grave.quickEquip(player);
                    } else {
                        grave.openUi(player, true, false);
                    }
                }
                return ActionResult.SUCCESS;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return playerTemp.isSneaking() ? ActionResult.PASS : ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return type == GraveBlockEntity.BLOCK_ENTITY_TYPE && !world.isClient ? GraveBlockEntity::tick : null;
    }

    @Override
    protected @Nullable Grave getGraveData(World world, BlockPos pos) {
        var entity = world.getBlockEntity(pos, GraveBlockEntity.BLOCK_ENTITY_TYPE);
        return entity.isPresent() ? entity.get().getGrave() : null;
    }

    @Override
    protected @Nullable Text[] getTextOverrides(World world, BlockPos pos) {
        return null;
    }

    @Override
    protected VisualGraveData getVisualData(World world, BlockPos pos, @Nullable Grave grave) {
        return grave != null ? grave.toVisualGraveData() : VisualGraveData.DEFAULT;
    }

    @Override
    protected Map<String, Text> getPlaceholders(MinecraftServer server, VisualGraveData visualGrave, @Nullable Grave grave) {
        return grave != null ? grave.getPlaceholders(server) : visualGrave.getPlaceholders(server);
    }
}
