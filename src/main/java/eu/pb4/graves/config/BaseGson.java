package eu.pb4.graves.config;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import eu.pb4.graves.config.data.IconData;
import eu.pb4.graves.config.data.WrappedDateFormat;
import eu.pb4.graves.config.data.WrappedText;
import eu.pb4.graves.model.TaggedText;
import eu.pb4.graves.model.parts.ModelPart;
import eu.pb4.graves.model.parts.ModelPartType;
import eu.pb4.graves.other.DynamicLevelUnlockCost;
import eu.pb4.graves.other.GenericCost;
import eu.pb4.graves.other.GraveUnlockCost;
import eu.pb4.graves.other.GravesXPCalculation;
import eu.pb4.predicate.api.GsonPredicateSerializer;
import eu.pb4.predicate.api.MinecraftPredicate;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Brightness;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class BaseGson {

    public static Gson getGson(HolderLookup.Provider lookup) {
        return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().enableComplexMapKeySerialization()
                .registerTypeHierarchyAdapter(Identifier.class, new CodecSerializer<>(Identifier.CODEC, lookup))

                .registerTypeHierarchyAdapter(Item.class, CodecSerializer.registry(BuiltInRegistries.ITEM, lookup))
                .registerTypeHierarchyAdapter(Block.class, CodecSerializer.registry(BuiltInRegistries.BLOCK, lookup))
                .registerTypeHierarchyAdapter(SoundEvent.class, CodecSerializer.registry(BuiltInRegistries.SOUND_EVENT, lookup))
                .registerTypeHierarchyAdapter(MobEffect.class, CodecSerializer.registry(BuiltInRegistries.MOB_EFFECT, lookup))
                .registerTypeHierarchyAdapter(EntityType.class, CodecSerializer.registry(BuiltInRegistries.ENTITY_TYPE, lookup))
                .registerTypeHierarchyAdapter(BlockEntityType.class, CodecSerializer.registry(BuiltInRegistries.BLOCK_ENTITY_TYPE, lookup))

                //.registerTypeHierarchyAdapter(ItemStack.class, new CodecSerializer<>(ItemStack.CODEC, lookup))
                .registerTypeHierarchyAdapter(ItemStack.class, new ItemStackSerializer(lookup))
                .registerTypeHierarchyAdapter(DataComponentMap.class, new CodecSerializer<>(DataComponentMap.CODEC, lookup))
                .registerTypeHierarchyAdapter(CompoundTag.class, new CodecSerializer<>(CompoundTag.CODEC, lookup))
                .registerTypeHierarchyAdapter(BlockPos.class, new CodecSerializer<>(BlockPos.CODEC, lookup))
                .registerTypeHierarchyAdapter(MinecraftPredicate.class, GsonPredicateSerializer.create(lookup))
                .registerTypeHierarchyAdapter(Vec3.class, new CodecSerializer<>(Vec3.CODEC, lookup))
                .registerTypeHierarchyAdapter(Vec2.class, new CodecSerializer<>(Codec.list(Codec.DOUBLE).xmap(x -> new Vec2(x.get(0).floatValue(), x.get(1).floatValue()), x -> List.of((double) x.x, (double) x.y)), lookup))
                .registerTypeHierarchyAdapter(EntityDimensions.class, new CodecSerializer<>(Codec.list(Codec.DOUBLE).xmap(x -> EntityDimensions.fixed(x.get(0).floatValue(), x.get(1).floatValue()), x -> List.of((double) x.width(), (double) x.height())), lookup))
                .registerTypeHierarchyAdapter(BlockState.class, new CodecSerializer<>(BlockState.CODEC, lookup))
                .registerTypeHierarchyAdapter(Transformation.class, new CodecSerializer<>(Transformation.CODEC, lookup))
                .registerTypeHierarchyAdapter(Display.BillboardConstraints.class, new CodecSerializer<>(Display.BillboardConstraints.CODEC, lookup))
                .registerTypeHierarchyAdapter(ParticleOptions.class, new CodecSerializer<>(ParticleTypes.CODEC, lookup))
                .registerTypeHierarchyAdapter(Display.TextDisplay.Align.class, new CodecSerializer<>(Display.TextDisplay.Align.CODEC, lookup))
                .registerTypeHierarchyAdapter(Brightness.class, new CodecSerializer<>(Brightness.CODEC, lookup))
                //.registerTypeHierarchyAdapter(Matrix4f.class, new CodecSerializer<>(AffineTransformation.ANY_CODEC.xmap(AffineTransformation::getMatrix, AffineTransformation::new)))

                .registerTypeHierarchyAdapter(GravesXPCalculation.class, new StringSerializer<>(GravesXPCalculation::byName, GravesXPCalculation::configName))
                .registerTypeHierarchyAdapter(GenericCost.class, new TeleportationCostSerializer())
                .registerTypeHierarchyAdapter(GraveUnlockCost.class, new GraveUnlockCostSerializer())
                .registerTypeHierarchyAdapter(IconData.class, new IconDataSerializer())
                .registerTypeHierarchyAdapter(WrappedText.class, new StringSerializer<>(WrappedText::of, WrappedText::input))
                .registerTypeHierarchyAdapter(TaggedText.class, new CodecSerializer<>(TaggedText.CODEC, lookup))
                .registerTypeHierarchyAdapter(WrappedDateFormat.class, new StringSerializer<>(WrappedDateFormat::of, WrappedDateFormat::pattern))
                .registerTypeAdapter(ModelPart.class, new ModelPartSerializer())
                .setLenient().create();
    }

    public static CodecSerializer<Component> text(HolderLookup.Provider lookup) {
        return CodecSerializer.ofText(lookup);
    }

    private record TeleportationCostSerializer() implements JsonSerializer<GenericCost<Object>>, JsonDeserializer<GenericCost<Object>> {

        @Override
        public GenericCost<Object> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject()) {
                return null;
            }

            var obj = jsonElement.getAsJsonObject();

            var baseType = obj.has("type") ? GenericCost.Type.BY_TYPE.getOrDefault(obj.get("type").getAsString(), GenericCost.Type.CREATIVE) : GenericCost.Type.CREATIVE;
            var input = baseType.decodeConfig(obj.get("input"), jsonDeserializationContext);
            var count = obj.has("count") ? obj.getAsJsonPrimitive("count").getAsInt() : 1;

            return new GenericCost(baseType, input, count);
        }

        @Override
        public JsonElement serialize(GenericCost<Object> teleportationCost, Type type, JsonSerializationContext jsonSerializationContext) {
            var obj = new JsonObject();
            obj.addProperty("type", GenericCost.Type.TYPE_NAME.get(teleportationCost.type()));

            var x = teleportationCost.type().encodeConfig(teleportationCost.object(), jsonSerializationContext);
            if (x != null) {
                obj.add("input", x);
            }
            obj.addProperty("count", teleportationCost.count());

            return obj;
        }
    }

    private record GraveUnlockCostSerializer() implements JsonSerializer<GraveUnlockCost>, JsonDeserializer<GraveUnlockCost> {
        @Override
        public GraveUnlockCost deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (!jsonElement.isJsonObject()) {
                return new GraveUnlockCost.Static(new GenericCost<>(GenericCost.Type.FREE, null, 0));
            }

            var object = jsonElement.getAsJsonObject();
            var typeName = object.has("type") ? object.get("type").getAsString() : "creative";
            if (!typeName.equals("dynamic_level")) {
                return new GraveUnlockCost.Static(context.deserialize(jsonElement, GenericCost.class));
            }

            return new DynamicLevelUnlockCost(
                    getInt(object, "stack_divisor", 3),
                    getInt(object, "enchantment_divisor", 4),
                    DynamicLevelUnlockCost.EnchantmentCostMode.fromConfig(getString(object, "enchantment_cost_mode", "levels")),
                    getInt(object, "minimum_cost", 1),
                    getDouble(object, "owner_multiplier", 1),
                    getDouble(object, "non_owner_multiplier", 3)
            );
        }

        @Override
        public JsonElement serialize(GraveUnlockCost cost, Type type, JsonSerializationContext context) {
            if (cost instanceof GraveUnlockCost.Static staticCost) {
                return context.serialize(staticCost.cost(), GenericCost.class);
            }

            var dynamicCost = (DynamicLevelUnlockCost) cost;
            var object = new JsonObject();
            object.addProperty("type", "dynamic_level");
            object.addProperty("stack_divisor", dynamicCost.stackDivisor());
            object.addProperty("enchantment_divisor", dynamicCost.enchantmentDivisor());
            object.addProperty("enchantment_cost_mode", dynamicCost.enchantmentCostMode().configName());
            object.addProperty("minimum_cost", dynamicCost.minimumCost());
            object.addProperty("owner_multiplier", dynamicCost.ownerMultiplier());
            object.addProperty("non_owner_multiplier", dynamicCost.nonOwnerMultiplier());
            return object;
        }

        private static int getInt(JsonObject object, String key, int fallback) {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        }

        private static double getDouble(JsonObject object, String key, double fallback) {
            return object.has(key) ? object.get(key).getAsDouble() : fallback;
        }

        private static String getString(JsonObject object, String key, String fallback) {
            return object.has(key) ? object.get(key).getAsString() : fallback;
        }
    }

    private record ModelPartSerializer() implements JsonSerializer<ModelPart<?, ?>>, JsonDeserializer<ModelPart<?, ?>> {


        @Override
        public ModelPart<?, ?> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            var obj = jsonElement.getAsJsonObject();

            var typeElement = ModelPartType.valueOf(obj.get("type").getAsString().toUpperCase(Locale.ROOT));

            return jsonDeserializationContext.deserialize(jsonElement, typeElement.modelPartClass);
        }

        @Override
        public JsonElement serialize(ModelPart<?, ?> modelPart, Type type, JsonSerializationContext jsonSerializationContext) {
            var obj = new JsonObject();
            obj.addProperty("type", modelPart.type().name());

            obj.asMap().putAll(jsonSerializationContext.serialize(modelPart, modelPart.getClass()).getAsJsonObject().asMap());
            return obj;
        }
    }

    private record IconDataSerializer() implements JsonSerializer<IconData>, JsonDeserializer<IconData> {
        @Override
        public IconData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject()) {
                return null;
            }

            var obj =  jsonElement.getAsJsonObject();

            var jsonText = obj.get("text");

            var texts = new ArrayList<String>();
            ItemStack itemStack = jsonDeserializationContext.deserialize(obj.get("icon"), ItemStack.class);

            if (jsonText.isJsonArray()) {
                for (JsonElement x : jsonText.getAsJsonArray()) {
                    texts.add(x.getAsString());
                }
            } else {
                texts.add(jsonText.getAsString());
            }

            return IconData.of(itemStack, texts);
        }

        @Override
        public JsonElement serialize(IconData iconData, Type type, JsonSerializationContext jsonSerializationContext) {
            var obj = new JsonObject();

            obj.add("icon", jsonSerializationContext.serialize(iconData.baseStack()));

            if (iconData.text().size() == 1) {
                obj.addProperty("text", iconData.text().get(0).input());
            } else {
                var list = new JsonArray();
                for (var x : iconData.text()) {
                    list.add(x.input());
                }
                obj.add("text", list);
            }

            return obj;
        }
    }

    private record ItemStackSerializer(HolderLookup.Provider lookup) implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
        @Override
        public ItemStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonObject()) {
                if (jsonElement.getAsJsonObject().has("tag")) {
                    jsonElement = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(JsonOps.INSTANCE, jsonElement), 3700, SharedConstants.getCurrentVersion().dataVersion().version()).getValue();
                }

                return ItemStack.CODEC.decode(RegistryOps.create(JsonOps.INSTANCE, lookup), jsonElement).result().orElse(Pair.of(ItemStack.EMPTY, null)).getFirst();
            } else {
                return BuiltInRegistries.ITEM.getValue(Identifier.tryParse(jsonElement.getAsString())).getDefaultInstance();
            }
        }

        @Override
        public JsonElement serialize(ItemStack stack, Type type, JsonSerializationContext jsonSerializationContext) {
            if (stack.getCount() == 1 && stack.getComponentsPatch().isEmpty()) {
                return new JsonPrimitive(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }

            return ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, lookup), stack).result().orElse(null);
        }
    }

    private record StringSerializer<T>(Function<String, T> decode, Function<T, String> encode) implements JsonSerializer<T>, JsonDeserializer<T> {
        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return this.decode.apply(json.getAsString());
            }
            return null;
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(this.encode.apply(src));
        }
    }

    public record CodecSerializer<T>(Codec<T> codec, HolderLookup.Provider lookup) implements JsonSerializer<T>, JsonDeserializer<T> {
        public static CodecSerializer<Component> ofText(HolderLookup.Provider lookup) {
            return new CodecSerializer<>(ComponentSerialization.CODEC, lookup);
        }

        public static <T> CodecSerializer<T> registry(Registry<T> registry, HolderLookup.Provider lookup) {
            return new CodecSerializer<>(registry.byNameCodec(), lookup);
        }

        public String toJsonString(T text) {
            return serialize(text, Component.class, null).toString();
        }

        public T fromJson(String string) {
            return deserialize(JsonParser.parseString(string), Component.class, null);
        }

        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return this.codec.decode(lookup.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow().getFirst();
            } catch (Throwable e) {
                return null;
            }
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            try {
                return src != null ? this.codec.encodeStart(lookup.createSerializationContext(JsonOps.INSTANCE), src).getOrThrow() : JsonNull.INSTANCE;
            } catch (Throwable e) {
                return JsonNull.INSTANCE;
            }
        }
    }
}
