package eu.pb4.graves.registry;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ContainerGraveBlock extends VisualGraveBlock {
    public static ContainerGraveBlock INSTANCE = new ContainerGraveBlock(AbstractBlock.Settings.create().nonOpaque().dynamicBounds().hardness(4));

    public ContainerGraveBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ContainerGraveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return ContainerGraveBlockEntity::tick;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (placer != null) {
            var optional = world.getBlockEntity(pos, ContainerGraveBlockEntity.BLOCK_ENTITY_TYPE);
            if (optional.isPresent()) {
                optional.get().textOverrides = new Text[]{Text.empty(), Text.empty(), Text.empty(), Text.empty()};
            }
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        ItemScatterer.onStateReplaced(state, newState, world, pos);
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity entity,  BlockHitResult hit) {
        if (entity instanceof ServerPlayerEntity player && !player.isSneaking()) {
            var blockEntityOptional = world.getBlockEntity(pos, ContainerGraveBlockEntity.BLOCK_ENTITY_TYPE);

            if (blockEntityOptional.isPresent() && blockEntityOptional.get().isPlayerMade) {
                var grave = blockEntityOptional.get();

                var itemStack = player.getStackInHand(Hand.MAIN_HAND);
                if (itemStack.getItem() == Items.FEATHER) {
                    grave.openEditScreen(player);
                } else if (itemStack.getItem() == Items.PLAYER_HEAD) {
                    if (itemStack.contains(DataComponentTypes.PROFILE)) {
                        grave.setVisualData(new VisualGraveData(
                                itemStack.get(DataComponentTypes.PROFILE).gameProfile(),
                                grave.getGraveSkinModelLayers(),
                                grave.getGraveMainArm(),
                                grave.getGrave().deathCause(),
                                grave.getGrave().creationTime(),
                                grave.getGrave().location(), grave.getGrave().minecraftDay()), grave.replacedBlockState);
                    } else {
                        grave.setVisualData(new VisualGraveData(
                                new GameProfile(Util.NIL_UUID, "Player"),
                                grave.getGraveSkinModelLayers(),
                                grave.getGraveMainArm(),
                                grave.getGrave().deathCause(),
                                grave.getGrave().creationTime(),
                                grave.getGrave().location(), grave.getGrave().minecraftDay()), grave.replacedBlockState);
                    }
                } else if (itemStack.getItem() == Items.MOSS_BLOCK) {
                    world.setBlockState(pos, state.with(IS_LOCKED, false));
                    grave.updateModel();
                } else if (itemStack.getItem() == Items.SPONGE || itemStack.getItem() == Items.WET_SPONGE) {
                    world.setBlockState(pos, state.with(IS_LOCKED, true));
                    grave.updateModel();
                } else if (itemStack.getItem() instanceof ShovelItem) {
                    int val = state.get(Properties.ROTATION) + (player.isSneaking() ? -1 : 1);
                    if (val < 0) {
                        val = RotationPropertyHelper.getMax();
                    } else if (val > RotationPropertyHelper.getMax()) {
                        val = 0;
                    }

                    world.setBlockState(pos, state.with(Properties.ROTATION, val));
                } else {
                    var gui = new SimpleGui(ScreenHandlerType.GENERIC_3X3, player, false) {
                        @Override
                        public void onTick() {
                            if (grave.isRemoved() || grave.getPos().getSquaredDistanceFromCenter(player.getX(), player.getY(), player.getZ()) > 256) {
                                this.close();
                            }
                        }

                        @Override
                        public void onClose() {
                            super.onClose();
                            player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_BARREL_CLOSE), SoundCategory.BLOCKS,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, world.random.nextFloat() * 0.1F + 0.9F, world.random.nextLong()));
                        }
                    };
                    gui.setTitle(this.getName());
                    for (int i = 0; i < 9; i++) {
                        gui.setSlotRedirect(i, new Slot(grave, i, 0, 0));
                    }
                    gui.open();
                    player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_BARREL_OPEN), SoundCategory.BLOCKS,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, world.random.nextFloat() * 0.1F + 0.9F, world.random.nextLong()));
                }

                return ActionResult.SUCCESS;
            }
        }

        return entity.isSneaking() ? ActionResult.PASS : ActionResult.SUCCESS;
    }

}
