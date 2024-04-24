package eu.pb4.graves.mixin.datafixer;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.Schema1460;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(Schema1460.class)
public abstract class Schema1460Mixin extends Schema {
    public Schema1460Mixin(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    @Shadow
    protected static void method_5273(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
    }

    @Inject(method = "registerBlockEntities", at = @At("RETURN"))
    private void registerPolyFactoryBlockEntities(Schema schema, CallbackInfoReturnable<Map<String, Supplier<TypeTemplate>>> cir) {
        var map = cir.getReturnValue();

        method_5273(schema, map, mod("container_grave"));

        schema.registerSimple(map, mod("grave"));
        schema.registerSimple(map, mod("visual_grave"));
    }

    @Unique
    private static String mod(String path) {
        return "universal_graves:" + path;
    }
}