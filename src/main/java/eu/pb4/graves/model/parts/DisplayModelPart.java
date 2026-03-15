package eu.pb4.graves.model.parts;

import com.google.gson.annotations.SerializedName;
import com.mojang.math.Transformation;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityDimensions;

public abstract class DisplayModelPart<T extends DisplayElement, G extends DisplayModelPart<T, G>> extends ModelPart<T, G> {
    @SerializedName("transformation")
    public Transformation transformation = Transformation.IDENTITY;
    @SerializedName("billboard")
    public Display.BillboardConstraints billboardMode = Display.BillboardConstraints.FIXED;
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

    @Override
    public T construct(ServerLevel world) {
        var base = constructBase();
        base.setOffset(this.position);
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
        base.setInvisible(ConfigManager.getConfig().model.hideF3DebugLines);
        return base;
    }

    protected abstract T constructBase();
}
