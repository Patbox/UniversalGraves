package eu.pb4.graves.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;

public record GraveCompassComponent(long graveId, boolean convertToVanilla)  {
    public static final ComponentType<GraveCompassComponent> TYPE = ComponentType.<GraveCompassComponent>builder()
            .codec(RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.fieldOf("id").forGetter(GraveCompassComponent::graveId),
                    Codec.BOOL.fieldOf("vanilla").forGetter(GraveCompassComponent::convertToVanilla)
            ).apply(instance, GraveCompassComponent::new))).build();
}
