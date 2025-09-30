package eu.pb4.graves.mixin;

import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerLikeEntity.class)
public interface PlayerLikeEntityAccessor {
    @Accessor
    static TrackedData<Byte> getPLAYER_MODE_CUSTOMIZATION_ID() {
        throw new UnsupportedOperationException();
    }
}
