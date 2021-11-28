package eu.pb4.graves.grave;

import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.mixin.PlayerInventoryAccessor;
import eu.pb4.polymer.api.block.PolymerBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.Wearable;
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
public class GraveBlock extends Block implements PolymerBlock, BlockEntityProvider, Waterloggable {
    public static BooleanProperty IS_LOCKED = BooleanProperty.of("is_locked");

    public static GraveBlock INSTANCE = new GraveBlock();

    private GraveBlock() {
        super(AbstractBlock.Settings.of(Material.METAL).dropsNothing().strength(2, 999));
        this.setDefaultState(this.getStateManager().getDefaultState().with(Properties.WATERLOGGED, false));
    }

    public static void insertStack(PlayerInventory inventory, ItemStack stack) {
        if (!stack.isEmpty()) {
            int slot;
            try {
                if (stack.isDamaged()) {
                    slot = inventory.getEmptySlot();


                    if (slot >= 0) {
                        inventory.main.set(slot, stack.copy());
                        stack.setCount(0);
                    }
                } else {
                    int i;
                    do {
                        i = stack.getCount();
                        stack.setCount(((PlayerInventoryAccessor) inventory).callAddStack(stack));
                    } while (!stack.isEmpty() && stack.getCount() < i);
                }
            } catch (Exception e) {
                // Silence!
            }
        }
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
        if (!(player instanceof ServerPlayerEntity) || hand == Hand.OFF_HAND) {
            return ActionResult.FAIL;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof GraveBlockEntity grave && grave.info.canTakeFrom(player)) {
            try {
                var config = ConfigManager.getConfig();
                if (grave.info.itemCount > 0) {
                    if (config.configData.shiftClickTakesItems && player.isSneaking()) {
                        for (int i = 0; i < grave.size(); i++) {
                            var stack = grave.getStack(i);
                            if (!stack.isEmpty()) {
                                if ((stack.getItem() instanceof Wearable || stack.getItem() instanceof ShieldItem) && stack.getCount() == 1 && !EnchantmentHelper.hasBindingCurse(stack)) {
                                    var slot = LivingEntity.getPreferredEquipmentSlot(stack);

                                    if (player.getEquippedStack(slot).isEmpty()) {
                                        player.equipStack(slot, stack.copy());
                                        stack.setCount(0);
                                        continue;
                                    }
                                }

                                insertStack(player.getInventory(), stack);
                            }
                        }
                        grave.updateState();
                    } else {
                        new GraveGui((ServerPlayerEntity) player, grave).open();
                    }
                } else if (ConfigManager.getConfig().configData.breakEmptyGraves) {
                    world.setBlockState(pos, grave.replacedBlockState, Block.NOTIFY_ALL);
                } else {
                    grave.clearGrave();
                }
                return ActionResult.SUCCESS;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return ConfigManager.getConfig().style.converter.getBlock(state.get(IS_LOCKED));
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return ConfigManager.getConfig().style.converter.getBlockState(state.get(Properties.ROTATION), state.get(IS_LOCKED), state.get(Properties.WATERLOGGED));
    }
    @Override
    public void onPolymerBlockSend(ServerPlayerEntity player, BlockPos.Mutable pos, BlockState state) {
        BlockEntity blockEntity = player.world.getBlockEntity(pos);

        if (blockEntity instanceof GraveBlockEntity grave) {
            ConfigManager.getConfig().style.converter.sendNbt(player, state, pos.toImmutable(), state.get(Properties.ROTATION), state.get(IS_LOCKED), grave.info);
        }
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
}
