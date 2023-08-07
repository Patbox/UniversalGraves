package eu.pb4.graves.model.parts;

import com.google.gson.annotations.SerializedName;
import eu.pb4.graves.model.TaggedText;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.entity.decoration.DisplayEntity;

public class TextDisplayModelPart extends DisplayModelPart<TextDisplayElement, TextDisplayModelPart> {
    @SerializedName("text")
    public TaggedText text = TaggedText.EMPTY;
    @SerializedName("text_width")
    public int textWidth = -1;
    @SerializedName("text_background")
    public int textBackground = -1;
    @SerializedName("text_opacity")
    public int textOpacity = -1;
    @SerializedName("text_shadow")
    public boolean textShadow = false;
    @SerializedName("text_see_through")
    public boolean textSeeThrough = false;
    @SerializedName("text_default_background")
    public boolean textDefaultBackground = true;
    @SerializedName("text_alignment")
    public DisplayEntity.TextDisplayEntity.TextAlignment textAlignment = DisplayEntity.TextDisplayEntity.TextAlignment.CENTER;


    @Override
    protected TextDisplayElement constructBase() {
        var e = new TextDisplayElement(this.text.direct());

        if (textWidth != -1) {
            e.setLineWidth(textWidth);
        }

        if (textBackground != -1) {
            e.setBackground(textBackground);
        }

        if (textOpacity != -1) {
            e.setTextOpacity((byte) textBackground);
        }

        e.setShadow(this.textShadow);
        e.setSeeThrough(this.textSeeThrough);
        e.setDefaultBackground(this.textDefaultBackground);
        e.setTextAlignment(this.textAlignment);
        return e;
    }

    @Override
    public ModelPartType type() {
        return ModelPartType.TEXT;
    }
}
