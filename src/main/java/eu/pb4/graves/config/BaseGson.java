package eu.pb4.graves.config;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import eu.pb4.graves.config.data.IconData;
import eu.pb4.graves.config.data.WrappedDateFormat;
import eu.pb4.graves.config.data.WrappedText;
import eu.pb4.graves.model.TaggedText;
import eu.pb4.graves.model.parts.ModelPart;
import eu.pb4.graves.model.parts.ModelPartType;
import eu.pb4.graves.other.GravesXPCalculation;
import eu.pb4.graves.other.GenericCost;
import eu.pb4.predicate.api.GsonPredicateSerializer;
import eu.pb4.predicate.api.MinecraftPredicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class BaseGson {
    private static final RegistryWrapper.WrapperLookup GLOBAL_REGISTRIES = DynamicRegistryManager.of(Registries.REGISTRIES);

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting()
            .registerTypeHierarchyAdapter(Identifier.class, new Identifier.Serializer())

            .registerTypeHierarchyAdapter(Item.class, new RegistrySerializer<>(Registries.ITEM))
            .registerTypeHierarchyAdapter(Block.class, new RegistrySerializer<>(Registries.BLOCK))
            .registerTypeHierarchyAdapter(Enchantment.class, new RegistrySerializer<>(Registries.ENCHANTMENT))
            .registerTypeHierarchyAdapter(SoundEvent.class, new RegistrySerializer<>(Registries.SOUND_EVENT))
            .registerTypeHierarchyAdapter(StatusEffect.class, new RegistrySerializer<>(Registries.STATUS_EFFECT))
            .registerTypeHierarchyAdapter(EntityType.class, new RegistrySerializer<>(Registries.ENTITY_TYPE))
            .registerTypeHierarchyAdapter(BlockEntityType.class, new RegistrySerializer<>(Registries.BLOCK_ENTITY_TYPE))

            .registerTypeHierarchyAdapter(Text.class, new Text.Serializer())

            //.registerTypeHierarchyAdapter(ItemStack.class, new CodecSerializer<>(ItemStack.CODEC))
            .registerTypeHierarchyAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeHierarchyAdapter(NbtCompound.class, new CodecSerializer<>(NbtCompound.CODEC))
            .registerTypeHierarchyAdapter(BlockPos.class, new CodecSerializer<>(BlockPos.CODEC))
            .registerTypeHierarchyAdapter(MinecraftPredicate.class, GsonPredicateSerializer.INSTANCE)
            .registerTypeHierarchyAdapter(Vec3d.class, new CodecSerializer<>(Vec3d.CODEC))
            .registerTypeHierarchyAdapter(Vec2f.class, new CodecSerializer<>(Codec.list(Codec.DOUBLE).xmap(x -> new Vec2f(x.get(0).floatValue(), x.get(1).floatValue()), x -> List.of((double) x.x, (double) x.y))))
            .registerTypeHierarchyAdapter(EntityDimensions.class, new CodecSerializer<>(Codec.list(Codec.DOUBLE).xmap(x -> EntityDimensions.fixed(x.get(0).floatValue(), x.get(1).floatValue()), x -> List.of((double) x.width, (double) x.height))))
            .registerTypeHierarchyAdapter(BlockState.class, new CodecSerializer<>(BlockState.CODEC))
            .registerTypeHierarchyAdapter(AffineTransformation.class, new CodecSerializer<>(AffineTransformation.CODEC))
            .registerTypeHierarchyAdapter(DisplayEntity.BillboardMode.class, new CodecSerializer<>(DisplayEntity.BillboardMode.CODEC))
            .registerTypeHierarchyAdapter(DisplayEntity.TextDisplayEntity.TextAlignment.class, new CodecSerializer<>(DisplayEntity.TextDisplayEntity.TextAlignment.CODEC))
            .registerTypeHierarchyAdapter(Brightness.class, new CodecSerializer<>(Brightness.CODEC))
            //.registerTypeHierarchyAdapter(Matrix4f.class, new CodecSerializer<>(AffineTransformation.ANY_CODEC.xmap(AffineTransformation::getMatrix, AffineTransformation::new)))

            .registerTypeHierarchyAdapter(GravesXPCalculation.class, new StringSerializer<>(GravesXPCalculation::byName, GravesXPCalculation::configName))
            .registerTypeHierarchyAdapter(GenericCost.class, new TeleportationCostSerializer())
            .registerTypeHierarchyAdapter(IconData.class, new IconDataSerializer())
            .registerTypeHierarchyAdapter(WrappedText.class, new StringSerializer<>(WrappedText::of, WrappedText::input))
            .registerTypeHierarchyAdapter(TaggedText.class, new CodecSerializer<>(TaggedText.CODEC))
            .registerTypeHierarchyAdapter(WrappedDateFormat.class, new StringSerializer<>(WrappedDateFormat::of, WrappedDateFormat::pattern))
            .registerTypeAdapter(ModelPart.class, new ModelPartSerializer())
            .setLenient().create();

    private record TeleportationCostSerializer() implements JsonSerializer<GenericCost<Object>>, JsonDeserializer<GenericCost<Object>> {

        @Override
        public GenericCost<Object> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject()) {
                return null;
            }

            var obj = jsonElement.getAsJsonObject();

            var baseType = obj.has("type") ? GenericCost.Type.BY_TYPE.getOrDefault(obj.get("type").getAsString(), GenericCost.Type.CREATIVE) : GenericCost.Type.CREATIVE;
            var input = baseType.decodeConfig(obj.get("input"));
            var count = obj.has("count") ? obj.getAsJsonPrimitive("count").getAsInt() : 1;

            return new GenericCost(baseType, input, count);
        }

        @Override
        public JsonElement serialize(GenericCost<Object> teleportationCost, Type type, JsonSerializationContext jsonSerializationContext) {
            var obj = new JsonObject();
            obj.addProperty("type", GenericCost.Type.TYPE_NAME.get(teleportationCost.type()));

            var x = teleportationCost.type().encodeConfig(teleportationCost.object());
            if (x != null) {
                obj.add("input", x);
            }
            obj.addProperty("count", teleportationCost.count());

            return obj;
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

    private record ItemStackSerializer() implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
        @Override
        public ItemStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonObject()) {
                return ItemStack.CODEC.decode(RegistryOps.of(JsonOps.INSTANCE, GLOBAL_REGISTRIES), jsonElement).result().orElse(Pair.of(ItemStack.EMPTY, null)).getFirst();
            } else {
                return Registries.ITEM.get(Identifier.tryParse(jsonElement.getAsString())).getDefaultStack();
            }
        }

        @Override
        public JsonElement serialize(ItemStack stack, Type type, JsonSerializationContext jsonSerializationContext) {
            if (stack.getCount() == 1 && !stack.hasNbt()) {
                return new JsonPrimitive(Registries.ITEM.getId(stack.getItem()).toString());
            }

            return ItemStack.CODEC.encodeStart(RegistryOps.of(JsonOps.INSTANCE, GLOBAL_REGISTRIES), stack).result().orElse(null);
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

    private record RegistrySerializer<T>(Registry<T> registry) implements JsonSerializer<T>, JsonDeserializer<T> {
        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return this.registry.get(Identifier.tryParse(json.getAsString()));
            }
            return null;
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive("" + this.registry.getId(src));
        }
    }

    private record CodecSerializer<T>(Codec<T> codec) implements JsonSerializer<T>, JsonDeserializer<T> {
        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return this.codec.decode(JsonOps.INSTANCE, json).getOrThrow(false, (x) -> {}).getFirst();
            } catch (Throwable e) {
                return null;
            }
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            try {
                return src != null ? this.codec.encodeStart(JsonOps.INSTANCE, src).getOrThrow(false, (x) -> {}) : JsonNull.INSTANCE;
            } catch (Throwable e) {
                return JsonNull.INSTANCE;
            }
        }
    }
}