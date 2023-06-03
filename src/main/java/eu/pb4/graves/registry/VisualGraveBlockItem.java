package eu.pb4.graves.registry;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.GraveNetworking;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.utils.PolymerClientDecoded;
import eu.pb4.polymer.core.api.utils.PolymerKeepModel;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;


public class VisualGraveBlockItem extends BlockItem implements PolymerItem, PolymerClientDecoded, PolymerKeepModel {
    public static final Item INSTANCE = new VisualGraveBlockItem();

    public VisualGraveBlockItem() {
        super(VisualGraveBlock.INSTANCE, new Settings());
    }

    protected boolean postPlacement(BlockPos pos, World world, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
        boolean bl = super.postPlacement(pos, world, player, stack, state);
        if (!world.isClient && !bl && player != null && player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof VisualGraveBlockEntity grave) {
            grave.openEditScreen(serverPlayer);

            grave.setVisualData(new VisualGraveData(
                    new GameProfile(MathHelper.randomUuid(), ""),
                    grave.getGrave().deathCause(),
                    grave.getGrave().creationTime(),
                    grave.getGrave().location(), grave.getGrave().minecraftDay()), grave.replacedBlockState, true);
        }

        return bl;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.STONE_BRICK_WALL;
    }
}
