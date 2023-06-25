package eu.pb4.graves.registry;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;


public class CointainerGraveBlockItem extends BlockItem implements PolymerItem {
    public static final Item INSTANCE = new CointainerGraveBlockItem();

    public CointainerGraveBlockItem() {
        super(ContainerGraveBlock.INSTANCE, new Settings());
    }

    protected boolean postPlacement(BlockPos pos, World world, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
        boolean bl = super.postPlacement(pos, world, player, stack, state);
        if (!world.isClient && !bl && player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof VisualGraveBlockEntity grave) {
            grave.openEditScreen(serverPlayer);

            grave.setVisualData(new VisualGraveData(
                    player != null && !player.isSneaking() ? player.getGameProfile() : new GameProfile(MathHelper.randomUuid(), ""),
                    grave.getGrave().deathCause(),
                    grave.getGrave().creationTime(),
                    grave.getGrave().location(), grave.getGrave().minecraftDay()), grave.replacedBlockState);
        }

        return bl;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return ConfigManager.getConfig().model.gravestoneItemBase;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipContext context, @Nullable ServerPlayerEntity player) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, context, player);
        var conf = ConfigManager.getConfig().model.gravestoneItemNbt;
        if (!conf.isEmpty()) {
            out.getOrCreateNbt().copyFrom(conf);
        }
        return out;
    }
}
