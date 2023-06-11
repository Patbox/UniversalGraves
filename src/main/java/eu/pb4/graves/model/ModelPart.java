package eu.pb4.graves.model;

import com.google.gson.annotations.SerializedName;
import eu.pb4.graves.config.BaseGson;
import eu.pb4.graves.config.data.WrappedText;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ModelPart {
    @Nullable
    @SerializedName("id")
    public Identifier id;

    @SerializedName("type")
    public Type type = Type.ITEM;
    @SerializedName("tags")
    public Set<Identifier> tags = new HashSet<>();
    @SerializedName("transformation")
    public AffineTransformation transformation = AffineTransformation.identity();
    @SerializedName("billboard")
    public DisplayEntity.BillboardMode billboardMode = DisplayEntity.BillboardMode.FIXED;
    @SerializedName("brightness")
    public Brightness brightness;
    @SerializedName("view_range")
    public float viewRange = 1;
    @SerializedName("shadow_radius")
    public float shadowRadius = 0;
    @SerializedName("shadow_strength")
    public float shadowStrength = 0;
    @SerializedName("culling")
    public EntityDimensions cullBox = EntityDimensions.fixed(2, 2);
    @SerializedName("glow_color")
    public int glowColor = -1;

    @SerializedName("display_item")
    public ItemStack itemStack = ItemStack.EMPTY;

    @SerializedName("item_model_transformation")
    public ModelTransformationMode itemModelTransformation = ModelTransformationMode.FIXED;

    @SerializedName("block_state")
    public BlockState blockState = Blocks.AIR.getDefaultState();

    @SerializedName("text")
    public WrappedText text = WrappedText.EMPTY;
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

    public DisplayElement construct() {
        DisplayElement base = switch (this.type) {
            case BLOCK -> new BlockDisplayElement(this.blockState);
            case ITEM -> {
                var e = new ItemDisplayElement(this.itemStack);
                e.setModelTransformation(this.itemModelTransformation);
                yield e;
            }
            case TEXT -> {
                var e = new TextDisplayElement(this.text.text());

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
                yield e;
            }
        };

        if (this.glowColor != -1) {
            base.setGlowing(true);
            base.setGlowColorOverride(this.glowColor);
        }

        base.setTransformation(this.transformation);
        base.setBillboardMode(this.billboardMode);
        if (this.brightness != null) {
            base.setBrightness(this.brightness);
        }
        base.setViewRange(this.viewRange);
        base.setDisplaySize(this.cullBox);
        base.setShadowRadius(this.shadowRadius);
        base.setShadowStrength(this.shadowStrength);
        base.setInvisible(true);

        return base;
    }

    public enum Type {
        BLOCK,
        ITEM,
        TEXT
    }

    public ModelPart copy() {
        // Ugly but quick
        return BaseGson.GSON.fromJson(BaseGson.GSON.toJsonTree(this), ModelPart.class);
    }

    public interface Tags {
        Identifier PLAYER_HEAD = new Identifier("graves", "player_head");
        Identifier IF_PROTECTED = new Identifier("graves", "if_protected");
        Identifier IF_UNPROTECTED = new Identifier("graves", "if_unprotected");
        Identifier IF_PLAYER_MADE = new Identifier("graves", "if_player_made");
        Identifier IF_NOT_PLAYER_MADE = new Identifier("graves", "if_not_player_made");
        Identifier IF_REQUIRE_PAYMENT = new Identifier("graves", "payment_required");
        Identifier IF_NOT_REQUIRE_PAYMENT = new Identifier("graves", "payment_not_required");
        Identifier IF_VISUAL = new Identifier("graves", "if_visual");
        Identifier IF_NOT_VISUAL = new Identifier("graves", "if_not_visual");
    }
}
