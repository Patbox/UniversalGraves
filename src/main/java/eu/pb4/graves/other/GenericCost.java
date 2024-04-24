package eu.pb4.graves.other;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import eu.pb4.graves.config.BaseGson;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public record GenericCost<T>(Type<T> type, T object, int count) {
    public boolean takeCost(ServerPlayerEntity player) {
        return player.isCreative() || this.type.checkCost(player, object, count, true);
    }

    public boolean checkCost(ServerPlayerEntity player) {
        return player.isCreative() || this.type.checkCost(player, object, count, false);
    }

    public void returnCost(ServerPlayerEntity player) {
        if (!player.isCreative()) {
            this.type.returnCost(player, this.object, count);
        }
    }

    public Map<String, Text> getPlaceholders() {
        return Map.of("cost", this.type.toText(this.object, this.count), "item", this.type.toName(this.object), "count", Text.literal("" + this.count));
    }

    public boolean isFree() {
        return type == Type.FREE;
    }

    @Override
    public String toString() {
        return "GenericCost{" +
                "type=" + Type.TYPE_NAME.get(type) +
                ", object=" + object +
                ", count=" + count +
                '}';
    }

    public Text toText() {
        return this.type.toText(this.object, this.count);
    }

    public interface Type<T> extends CostFunc<T> {
        Map<String, Type<?>> BY_TYPE = new HashMap<>();
        Map<Type<?>, String> TYPE_NAME = new HashMap<>();

        Type<Object> CREATIVE = reg("creative", of(Items.COMMAND_BLOCK, true, (p, c, x) -> p.isCreative(), (p, c) -> {}));
        Type<Object> FREE = reg("free", of(Items.COMMAND_BLOCK, true, (p, c, x) -> true, (p, c) -> {}));

        Type<Object> LEVEL = reg("level", of(Items.EXPERIENCE_BOTTLE, false, (p, c, x) -> {
            if (p.experienceLevel >= c) {
                if (x) {
                    p.experienceLevel -= c;
                    p.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(p.experienceProgress, p.totalExperience, p.experienceLevel));
                }
                return true;
            } else {
                return false;
            }
        }, (p, c) -> {
            p.experienceLevel += c;
            p.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(p.experienceProgress, p.totalExperience, p.experienceLevel));
        }));

        Type<ItemStack> ITEM = reg("item", new Type<>() {
            @Override
            public boolean checkCost(ServerPlayerEntity player, ItemStack object, int count, boolean take) {
                var c = 0;
                for (var i = 0; i < player.getInventory().size(); i++) {
                    var stack = player.getInventory().getStack(i);

                    if (ItemStack.areItemsAndComponentsEqual(stack, object)) {
                        c += stack.getCount();
                    }
                }

                if (c >= count) {
                    if (take) {
                        player.getInventory().remove((i) -> !i.isEmpty() && ItemStack.areItemsAndComponentsEqual(i, object), count, GraveUtils.EMPTY_INVENTORY);
                    }
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public ItemStack decodeConfig(JsonElement object, JsonDeserializationContext jsonDeserializationContext) {
                if (object != null) {
                    var x = jsonDeserializationContext.deserialize(object, ItemStack.class);
                    if (x != null) {
                        return (ItemStack) x;
                    }
                }
                return ItemStack.EMPTY;
            }

            @Override
            public JsonElement encodeConfig(ItemStack object, JsonSerializationContext jsonSerializationContext) {
                return jsonSerializationContext.serialize(object);
            }

            @Override
            public ItemStack getIcon(ItemStack object, int count) {
                return object.copy();
            }

            @Override
            public Text toName(ItemStack object) {
                return object.getName();
            }

            @Override
            public void returnCost(ServerPlayerEntity player, ItemStack object, int count) {
                var copy = object.copy();
                copy.setCount(count);
                player.giveItemStack(copy);
            }
        });

        static <T> Type<T> reg(String id, Type<T> type) {
            BY_TYPE.put(id, type);
            TYPE_NAME.put(type, id);
            return type;
        }

        static Type<Object> of(Item icon, boolean singular, ContextlessCost takeCost, ReturnCostFunc returnCostFunc) {
            return new Type<>() {

                @Override
                public Object decodeConfig(JsonElement object, JsonDeserializationContext jsonDeserializationContext) {
                    return null;
                }

                @Override
                public JsonElement encodeConfig(Object object, JsonSerializationContext jsonSerializationContext) {
                    return null;
                }

                @Override
                public ItemStack getIcon(Object object, int count) {
                    return icon.getDefaultStack();
                }

                @Override
                public Text toName(Object object) {
                    return Text.translatable("text.graves.cost." + TYPE_NAME.get(this));
                }

                @Override
                public Text toText(Object object, int i) {
                    return singular ? toName(object) : Type.super.toText(object, i);
                }

                public boolean checkCost(ServerPlayerEntity player, Object object, int count, boolean take) {
                    return takeCost.checkCost(player, count, take);
                }

                @Override
                public void returnCost(ServerPlayerEntity player, Object object, int count) {
                    returnCostFunc.returnCost(player, count);
                }
            };
        }

        T decodeConfig(@Nullable JsonElement object, JsonDeserializationContext jsonDeserializationContext);
        JsonElement encodeConfig(T object, JsonSerializationContext jsonSerializationContext);
        ItemStack getIcon(T object, int count);
        default Text toText(T object, int i) {
            return Text.empty().append(toName(object)).append(" × ").append("" + i);
        }
        Text toName(T object);

        void returnCost(ServerPlayerEntity player, T object, int count);

        interface ContextlessCost {
            boolean checkCost(ServerPlayerEntity player, int count, boolean take);
        }
        interface ReturnCostFunc {
            void returnCost(ServerPlayerEntity player, int count);
        }
    }

    public interface CostFunc<T> {
        boolean checkCost(ServerPlayerEntity player, T object, int count, boolean take);
    }
}
