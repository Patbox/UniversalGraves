package eu.pb4.graves.mixin;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {
    @Accessor
    static TrackedData<Byte> getPLAYER_MODEL_PARTS() {
        throw new UnsupportedOperationException();
    }
}
