package eu.pb4.graves.mixin.datafixer;

import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.fix.ItemStackComponentizationFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(ItemStackComponentizationFix.class)
public class ItemStackComponentizationFixMixin {
    @Inject(method = "fixStack", at = @At("TAIL"))
    private static void fixCustomStacks(ItemStackComponentizationFix.StackData data, Dynamic dynamic, CallbackInfo ci) {
        if (data.itemEquals("universal_graves:icon")) {
            data.moveToComponent("Texture", "universal_graves:texture", dynamic.createString(""));
        } else if (data.itemEquals("universal_graves:grave_compass")) {
            data.setComponent("universal_graves:compass", dynamic.emptyMap()
                    .set("id", data.getAndRemove("GraveId").result().orElse(dynamic.createLong(-1)))
                    .set("vanilla", data.getAndRemove("ConvertToVanillaGraveId").result().orElse(dynamic.createBoolean(false)))
            );
        }
    }
}
