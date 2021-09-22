package eu.pb4.graves.grave;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.polymer.block.VirtualBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"deprecation"})
public class GraveBlock extends Block implements VirtualBlock, BlockEntityProvider {
    public static BooleanProperty IS_LOCKED = BooleanProperty.of("is_locked");

    public static GraveBlock INSTANCE = new GraveBlock();

    private GraveBlock() {
        super(AbstractBlock.Settings.of(Material.METAL).dropsNothing().strength(2, 999));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.ROTATION).add(IS_LOCKED);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return false;
    }

    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.BLOCK;
    }

    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof GraveBlockEntity grave) {
                grave.onBroken();
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient() || hand == Hand.OFF_HAND) {
            return ActionResult.FAIL;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof GraveBlockEntity grave && grave.info.canTakeFrom(player)) {
            if (grave.info.itemCount > 0) {
                new GraveGui((ServerPlayerEntity) player, grave).open();
            } else {
                world.setBlockState(pos, grave.replacedBlockState, Block.NOTIFY_ALL);
            }
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public Block getVirtualBlock() {
        return ConfigManager.getConfig().style.converter.getBlock(false);
    }

    @Override
    public Block getVirtualBlock(BlockPos pos, World world) {
        return ConfigManager.getConfig().style.converter.getBlock(world.getBlockState(pos).get(IS_LOCKED));
    }

    public BlockState getDefaultVirtualBlockState() {
        return ConfigManager.getConfig().style.converter.getBlockState(0, true);
    }

    public BlockState getVirtualBlockState(BlockState state) {
        return ConfigManager.getConfig().style.converter.getBlockState(state.get(Properties.ROTATION), state.get(IS_LOCKED));
    }

    public void sendPacketsAfterCreation(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = player.world.getBlockEntity(pos);

        if (blockEntity instanceof GraveBlockEntity grave) {
            ConfigManager.getConfig().style.converter.sendNbt(player, state, pos, state.get(Properties.ROTATION), state.get(IS_LOCKED), grave.info.gameProfile);
        }
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
}
