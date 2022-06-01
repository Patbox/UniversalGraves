package eu.pb4.graves.registry;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.sgui.api.gui.SignGui;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings({"deprecation"})
public class VisualGraveBlock extends AbstractGraveBlock implements BlockEntityProvider {
    public static VisualGraveBlock INSTANCE = new VisualGraveBlock();

    private VisualGraveBlock() {
        super(Settings.copy(GraveBlock.INSTANCE).hardness(4));
        this.setDefaultState(this.getStateManager().getDefaultState().with(Properties.WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.ROTATION, IS_LOCKED, Properties.WATERLOGGED);
    }

    public PistonBehavior getPistonBehavior(BlockState state) {
        return PistonBehavior.BLOCK;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (placer != null) {
            var optional = world.getBlockEntity(pos, VisualGraveBlockEntity.BLOCK_ENTITY_TYPE);
            if (optional.isPresent()) {
                optional.get().textOverrides = new Text[]{LiteralText.EMPTY, LiteralText.EMPTY, LiteralText.EMPTY, LiteralText.EMPTY};
            }
        }
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        var optional = world.getBlockEntity(pos, VisualGraveBlockEntity.BLOCK_ENTITY_TYPE);
        if (world instanceof ServerWorld && optional.isPresent() && optional.get().allowModification) {
            world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, VisualGraveBlockItem.INSTANCE.getDefaultStack()));
        }
        super.onBreak(world, pos, state, player);
    }

    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected @Nullable Grave getGraveData(World world, BlockPos pos) {
        return null;
    }

    @Override
    protected @Nullable Text[] getTextOverrides(World world, BlockPos pos) {
        var entity = world.getBlockEntity(pos, VisualGraveBlockEntity.BLOCK_ENTITY_TYPE);
        return entity.isPresent() ? entity.get().textOverrides : null;
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
        return super.getPlacementState(ctx).with(Properties.ROTATION, (int) (Math.random() * 16));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity entity, Hand hand, BlockHitResult hit) {
        if (entity instanceof ServerPlayerEntity player) {
            var blockEntityOptional = world.getBlockEntity(pos, VisualGraveBlockEntity.BLOCK_ENTITY_TYPE);

            if (blockEntityOptional.isPresent() && blockEntityOptional.get().allowModification) {
                var grave = blockEntityOptional.get();

                var itemStack = player.getStackInHand(hand);
                if (itemStack.getItem() == Items.FEATHER) {

                    var sign = new SignGui(player) {
                        @Override
                        public void onClose() {
                            grave.textOverrides = new Text[]{
                                    this.getLine(0),
                                    this.getLine(1),
                                    this.getLine(2),
                                    this.getLine(3)
                            };
                        }
                    };
                    sign.setSignType(Blocks.BIRCH_SIGN);

                    if (grave.textOverrides != null) {
                        int i = 0;

                        for (var text : grave.textOverrides) {
                            sign.setLine(i, text.shallowCopy());
                            i++;

                            if (i == 4) {
                                break;
                            }
                        }
                    }

                    sign.open();
                } else if (itemStack.getItem() == Items.PLAYER_HEAD) {
                    if (itemStack.hasNbt() && itemStack.getNbt().contains(SkullItem.SKULL_OWNER_KEY, NbtElement.COMPOUND_TYPE)) {
                        grave.setVisualData(new VisualGraveData(
                                NbtHelper.toGameProfile(itemStack.getNbt().getCompound(SkullItem.SKULL_OWNER_KEY)),
                                grave.getGrave().deathCause(),
                                grave.getGrave().creationTime(),
                                grave.getGrave().location(), grave.getGrave().minecraftDay()), grave.replacedBlockState, true);
                    } else if (itemStack.hasNbt() && itemStack.getNbt().contains(SkullItem.SKULL_OWNER_KEY, NbtElement.STRING_TYPE)) {
                        player.getServer().getUserCache().findByNameAsync(itemStack.getNbt().getString(SkullItem.SKULL_OWNER_KEY), (profile) -> {
                            if (profile.isPresent()) {
                                grave.setVisualData(new VisualGraveData(
                                        profile.get(),
                                        grave.getGrave().deathCause(),
                                        grave.getGrave().creationTime(),
                                        grave.getGrave().location(), grave.getGrave().minecraftDay()), grave.replacedBlockState, true);
                            }
                        });
                    } else {
                        grave.setVisualData(new VisualGraveData(
                                new GameProfile(Util.NIL_UUID, "Player"),
                                grave.getGrave().deathCause(),
                                grave.getGrave().creationTime(),
                                grave.getGrave().location(), grave.getGrave().minecraftDay()), grave.replacedBlockState, true);
                    }
                } else if (itemStack.getItem() == Items.MOSS_BLOCK) {
                    world.setBlockState(pos, state.with(IS_LOCKED, false));
                } else if (itemStack.getItem() == Items.SPONGE || itemStack.getItem() == Items.WET_SPONGE) {
                    world.setBlockState(pos, state.with(IS_LOCKED, true));
                } else if (itemStack.getItem() instanceof ShovelItem) {
                    int val = state.get(Properties.ROTATION) + (player.isSneaking() ? -1 : 1);
                    if (val < 0) {
                        val = Properties.ROTATION_MAX;
                    } else if (val > 15) {
                        val = 0;
                    }

                    world.setBlockState(pos, state.with(Properties.ROTATION, val));
                }

                return ActionResult.SUCCESS;
            }
        }

        return entity.isSneaking() ? ActionResult.PASS : ActionResult.SUCCESS;
    }

}
