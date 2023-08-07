package eu.pb4.graves.model.parts;

import com.google.gson.annotations.SerializedName;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class BlockDisplayModelPart extends DisplayModelPart<BlockDisplayElement, BlockDisplayModelPart> {
    @SerializedName("block_state")
    public BlockState blockState = Blocks.AIR.getDefaultState();

    @Override
    protected BlockDisplayElement constructBase() {
        return new BlockDisplayElement(this.blockState);
    }

    @Override
    public ModelPartType type() {
        return ModelPartType.BLOCK;
    }
}
