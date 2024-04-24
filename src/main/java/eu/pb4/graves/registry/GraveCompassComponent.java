package eu.pb4.graves.registry;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polymer.core.api.item.PolymerItemComponent;
import net.minecraft.component.DataComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public record GraveCompassComponent(long graveId, boolean convertToVanilla) implements PolymerItemComponent {
    public static final DataComponentType<GraveCompassComponent> TYPE = DataComponentType.<GraveCompassComponent>builder()
            .codec(RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.fieldOf("id").forGetter(GraveCompassComponent::graveId),
                    Codec.BOOL.fieldOf("vanilla").forGetter(GraveCompassComponent::convertToVanilla)
            ).apply(instance, GraveCompassComponent::new))).build();
}
