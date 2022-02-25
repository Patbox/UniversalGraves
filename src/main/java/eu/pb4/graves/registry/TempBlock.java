package eu.pb4.graves.registry;

import eu.pb4.polymer.api.block.PolymerBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemPlacementContext;

public class TempBlock extends Block implements PolymerBlock {
    public static TempBlock INSTANCE = new TempBlock();

    public TempBlock() {
        super(FabricBlockSettings.copy(Blocks.BEDROCK).noCollision());
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return false;
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.AIR;
    }
}
