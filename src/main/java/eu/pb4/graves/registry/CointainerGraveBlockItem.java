package eu.pb4.graves.registry;

import com.mojang.authlib.GameProfile;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.mixin.PlayerLikeEntityAccessor;
import eu.pb4.graves.other.VisualGraveData;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;


public class CointainerGraveBlockItem extends BlockItem implements PolymerItem {
    public CointainerGraveBlockItem(Block block, Properties settings) {
        super(block, settings);
    }

    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level world, @Nullable Player player, ItemStack stack, BlockState state) {
        boolean bl = super.updateCustomBlockEntityTag(pos, world, player, stack, state);
        if (!world.isClientSide() && !bl && player instanceof ServerPlayer serverPlayer && world.getBlockEntity(pos) instanceof VisualGraveBlockEntity grave) {
            grave.openEditScreen(serverPlayer);

            grave.setVisualData(new VisualGraveData(
                    ResolvableProfile.createResolved(!player.isShiftKeyDown() ? player.getGameProfile() : new GameProfile(Mth.createInsecureUUID(RandomSource.create()), "")),
                    player.getEntityData().get(PlayerLikeEntityAccessor.getDATA_PLAYER_MODE_CUSTOMISATION()),
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
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        return null;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context, HolderLookup.Provider lookup) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context, lookup);
        var conf = ConfigManager.getConfig().model.gravestoneItemNbt;
        if (!conf.isEmpty()) {
            out.applyComponents(conf);
        }
        return out;
    }
}
