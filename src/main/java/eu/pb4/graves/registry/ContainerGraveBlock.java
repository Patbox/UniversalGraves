package eu.pb4.graves.registry;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ContainerGraveBlock extends VisualGraveBlock {
    public ContainerGraveBlock(Properties settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ContainerGraveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return ContainerGraveBlockEntity::tick;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (placer != null) {
            var optional = world.getBlockEntity(pos, ContainerGraveBlockEntity.BLOCK_ENTITY_TYPE);
            if (optional.isPresent()) {
                optional.get().textOverrides = new Component[]{Component.empty(), Component.empty(), Component.empty(), Component.empty()};
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        Containers.updateNeighboursAfterDestroy(state, world, pos);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player entity,  BlockHitResult hit) {
        if (entity instanceof ServerPlayer player && !player.isShiftKeyDown()) {
            var blockEntityOptional = world.getBlockEntity(pos, ContainerGraveBlockEntity.BLOCK_ENTITY_TYPE);

            if (blockEntityOptional.isPresent() && blockEntityOptional.get().isPlayerMade) {
                var grave = blockEntityOptional.get();

                var itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (itemStack.getItem() == Items.FEATHER) {
                    grave.openEditScreen(player);
                } else if (itemStack.getItem() == Items.PLAYER_HEAD) {
                    if (itemStack.has(DataComponents.PROFILE)) {
                        grave.setVisualData(new VisualGraveData(
                                itemStack.get(DataComponents.PROFILE),
                                grave.getGraveSkinModelLayers(),
                                grave.getGraveMainArm(),
                                grave.getGrave().deathCause(),
                                grave.getGrave().creationTime(),
                                grave.getGrave().location(), grave.getGrave().minecraftDay()), grave.replacedBlockState);
                    } else {
                        grave.setVisualData(new VisualGraveData(
                                ResolvableProfile.createResolved(new GameProfile(Util.NIL_UUID, "Player")),
                                grave.getGraveSkinModelLayers(),
                                grave.getGraveMainArm(),
                                grave.getGrave().deathCause(),
                                grave.getGrave().creationTime(),
                                grave.getGrave().location(), grave.getGrave().minecraftDay()), grave.replacedBlockState);
                    }
                } else if (itemStack.getItem() == Items.MOSS_BLOCK) {
                    world.setBlockAndUpdate(pos, state.setValue(IS_LOCKED, false));
                    grave.updateModel();
                } else if (itemStack.getItem() == Items.SPONGE || itemStack.getItem() == Items.WET_SPONGE) {
                    world.setBlockAndUpdate(pos, state.setValue(IS_LOCKED, true));
                    grave.updateModel();
                } else if (itemStack.getItem() instanceof ShovelItem) {
                    int val = state.getValue(BlockStateProperties.ROTATION_16) + (player.isShiftKeyDown() ? -1 : 1);
                    if (val < 0) {
                        val = RotationSegment.getMaxSegmentIndex();
                    } else if (val > RotationSegment.getMaxSegmentIndex()) {
                        val = 0;
                    }

                    world.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.ROTATION_16, val));
                } else {
                    var gui = new SimpleGui(MenuType.GENERIC_3x3, player, false) {
                        @Override
                        public void onTick() {
                            if (grave.isRemoved() || grave.getBlockPos().distToCenterSqr(player.getX(), player.getY(), player.getZ()) > 256) {
                                this.close();
                            }
                        }

                        @Override
                        public void onClose() {
                            super.onClose();
                            player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.BARREL_CLOSE), SoundSource.BLOCKS,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, world.getRandom().nextFloat() * 0.1F + 0.9F, world.getRandom().nextLong()));
                        }
                    };
                    gui.setTitle(this.getName());
                    for (int i = 0; i < 9; i++) {
                        gui.setSlotRedirect(i, new Slot(grave, i, 0, 0));
                    }
                    gui.open();
                    player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.BARREL_OPEN), SoundSource.BLOCKS,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, world.getRandom().nextFloat() * 0.1F + 0.9F, world.getRandom().nextLong()));
                }

                return InteractionResult.SUCCESS_SERVER;
            }
        }

        return entity.isShiftKeyDown() ? InteractionResult.PASS : InteractionResult.SUCCESS_SERVER;
    }

}
