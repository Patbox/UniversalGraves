package eu.pb4.graves.registry;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.mixin.PlayerEntityAccessor;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;


public class CointainerGraveBlockItem extends BlockItem implements PolymerItem {
    public CointainerGraveBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    protected boolean postPlacement(BlockPos pos, World world, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
        boolean bl = super.postPlacement(pos, world, player, stack, state);
        if (!world.isClient && !bl && player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof VisualGraveBlockEntity grave) {
            grave.openEditScreen(serverPlayer);

            grave.setVisualData(new VisualGraveData(
                    !player.isSneaking() ? player.getGameProfile() : new GameProfile(MathHelper.randomUuid(), ""),
                    player.getDataTracker().get(PlayerEntityAccessor.getPLAYER_MODEL_PARTS()),
                    player.getMainArm(),
                    grave.getGrave().deathCause(),
                    grave.getGrave().creationTime(),
                    grave.getGrave().location(), grave.getGrave().minecraftDay()), grave.replacedBlockState);
        }

        return bl;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return ConfigManager.getConfig().model.gravestoneItemBase;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return null;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
        var conf = ConfigManager.getConfig().model.gravestoneItemNbt;
        if (!conf.isEmpty()) {
            out.applyComponentsFrom(conf);
        }
        return out;
    }
}
