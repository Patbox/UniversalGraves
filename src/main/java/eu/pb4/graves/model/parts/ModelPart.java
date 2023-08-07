package eu.pb4.graves.model.parts;

import com.google.gson.annotations.SerializedName;
import eu.pb4.graves.config.BaseGson;
import eu.pb4.polymer.virtualentity.api.elements.AbstractElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public abstract class ModelPart<T extends AbstractElement, G extends ModelPart<T, G>> {
    @SerializedName("id")
    public @Nullable Identifier id;
    @SerializedName("tags")
    public Set<Identifier> tags = new HashSet<>();
    @SerializedName("rotate_position")
    public boolean rotatePos = true;
    @SerializedName("rotate_yaw")
    public boolean rotateYaw = true;
    @SerializedName("position")
    public Vec3d position = Vec3d.ZERO;

    public abstract T construct(ServerWorld world);

    public abstract ModelPartType type();

    public G copy() {
        // Ugly but quick
        //noinspection unchecked
        return (G) BaseGson.GSON.fromJson(BaseGson.GSON.toJsonTree(this), this.getClass());
    }
}
