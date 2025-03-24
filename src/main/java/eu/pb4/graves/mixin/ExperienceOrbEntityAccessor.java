package eu.pb4.graves.mixin;

import net.minecraft.entity.ExperienceOrbEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ExperienceOrbEntity.class)
public interface ExperienceOrbEntityAccessor {
    @Invoker
    void callSetValue(int amount);
}
