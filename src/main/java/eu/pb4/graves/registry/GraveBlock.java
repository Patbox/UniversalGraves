package eu.pb4.graves.registry;

import eu.pb4.graves.GravesMod;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.other.VisualGraveData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class GraveBlock extends AbstractGraveBlock implements EntityBlock {
    public GraveBlock(BlockBehaviour.Properties settings) {
        super(settings.noLootTable().noOcclusion().dynamicShape().strength(2, 999));
        this.registerDefaultState(this.getStateDefinition().any().setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof GraveBlockEntity be
                && (be.getGrave() == null || be.getGrave().canTakeFrom(player))) {
            return super.getDestroyProgress(state, player, world, pos);
        }

        return -1;
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player playerTemp) {
        if (!(playerTemp instanceof ServerPlayer player)) {
            return state;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof GraveBlockEntity graveBlockEntity && graveBlockEntity.getGrave() != null && graveBlockEntity.getGrave().canTakeFrom(player)) {
            try {
                var grave = graveBlockEntity.getGrave();
                if (ConfigManager.getConfig().interactions.breakingTakesItems && grave.isOwner(player)) {
                    grave.quickEquip(player);
                }
                grave.destroyGrave(player.level().getServer(), player);
                if (ConfigManager.getConfig().placement.restoreBlockAfterPlayerBreaking) {
                    graveBlockEntity.breakBlock();
                }
            } catch (Exception e) {
                GravesMod.LOGGER.error("Exception occurred while breaking grave!", e);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player playerTemp, BlockHitResult hit) {
        if (!(playerTemp instanceof ServerPlayer player)) {
            return playerTemp.isShiftKeyDown() ? InteractionResult.PASS : InteractionResult.FAIL;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!stack.isEmpty() && playerTemp.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (blockEntity instanceof GraveBlockEntity graveBlockEntity && graveBlockEntity.getGrave() != null
                && (graveBlockEntity.getGrave().hasAccess(player) || graveBlockEntity.getGrave().canPayForUnlock(player))) {
            try {
                var grave = graveBlockEntity.getGrave();

                grave.updateSelf(world.getServer());

                if (!grave.isRemoved()) {
                    if ((grave.canTakeFrom(player) || grave.hasProtectionAccess(player))
                            && ConfigManager.getConfig().interactions.shiftClickTakesItems
                            && (player.isShiftKeyDown() || !ConfigManager.getConfig().interactions.clickGraveToOpenGui)) {
                        grave.quickEquip(player);
                    } else if (!player.isShiftKeyDown()) {
                        grave.openUi(player, true, false);
                    } else {
                        return InteractionResult.PASS;
                    }
                }
                return InteractionResult.SUCCESS_SERVER;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return type == GraveBlockEntity.BLOCK_ENTITY_TYPE && !world.isClientSide() ? GraveBlockEntity::tick : null;
    }

    @Override
    protected @Nullable Grave getGraveData(Level world, BlockPos pos) {
        var entity = world.getBlockEntity(pos, GraveBlockEntity.BLOCK_ENTITY_TYPE);
        return entity.isPresent() ? entity.get().getGrave() : null;
    }

    @Override
    protected VisualGraveData getVisualData(Level world, BlockPos pos, @Nullable Grave grave) {
        return grave != null ? grave.toVisualGraveData() : VisualGraveData.DEFAULT;
    }

    @Override
    protected Map<String, Component> getPlaceholders(MinecraftServer server, VisualGraveData visualGrave, @Nullable Grave grave) {
        return grave != null ? grave.getPlaceholders(server) : visualGrave.getPlaceholders(server);
    }
}
