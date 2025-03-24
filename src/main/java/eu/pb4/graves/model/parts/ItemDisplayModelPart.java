package eu.pb4.graves.model.parts;

import com.google.gson.annotations.SerializedName;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;

public class ItemDisplayModelPart extends DisplayModelPart<ItemDisplayElement, ItemDisplayModelPart> {
    @SerializedName("display_item")
    public ItemStack itemStack = ItemStack.EMPTY;

    @SerializedName("item_model_transformation")
    public ItemDisplayContext itemModelTransformation = ItemDisplayContext.FIXED;


    @Override
    protected ItemDisplayElement constructBase() {
        var e = new ItemDisplayElement(this.itemStack.copy());
        e.setItemDisplayContext(this.itemModelTransformation);
        return e;
    }

    @Override
    public ModelPartType type() {
        return ModelPartType.ITEM;
    }
}
