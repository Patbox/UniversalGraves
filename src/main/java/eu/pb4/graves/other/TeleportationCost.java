package eu.pb4.graves.other;

import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public record TeleportationCost<T>(Type<T> type, T object, int count) {
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

    public Text getText(ServerPlayerEntity player) {
        var config = ConfigManager.getConfig();

        var text = this.checkCost(player) ? config.guiTeleportCostActiveText : config.guiTeleportCostNotEnoughText;

        var out = Placeholders.parseText(text, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN,
                Map.of("item", this.type.textify(this.object), "count", Text.literal(""+ this.count)));


        return out;
    }

    public static TeleportationCost<Object> decode(String typeString, String objectString, int count) {
        var type = Type.BY_TYPE.getOrDefault(typeString, Type.CREATIVE);
        var obj = type.convert(objectString);

        return new TeleportationCost(type, obj, count);
    }

    public interface Type<T> extends CostFunc<T> {
        Map<String, Type<?>> BY_TYPE = new HashMap<>();
        Map<Type<?>, String> TYPE_NAME = new HashMap<>();

        Type<Object> CREATIVE = reg("creative", of(Items.COMMAND_BLOCK, (p, c, x) -> p.isCreative(), (p, c) -> {}));
        Type<Object> FREE = reg("free", of(Items.COMMAND_BLOCK, (p, c, x) -> true, (p, c) -> {}));

        Type<Object> LEVEL = reg("level", of(Items.EXPERIENCE_BOTTLE, (p, c, x) -> {
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

                    if (ItemStack.canCombine(stack, object)) {
                        c += stack.getCount();
                    }
                }

                if (c >= count) {
                    if (take) {
                        player.getInventory().remove((i) -> !i.isEmpty() && ItemStack.canCombine(i, object), count, GraveUtils.EMPTY_INVENTORY);
                    }
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public ItemStack convert(String object) {
                return Config.parseItem(object);
            }

            @Override
            public ItemStack getIcon(ItemStack object, int count) {
                return object.copy();
            }

            @Override
            public Text textify(ItemStack object) {
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

        static Type<Object> of(Item icon, ContextlessCost takeCost, ReturnCostFunc returnCostFunc) {
            return new Type<>() {
                @Override
                public Object convert(String object) {
                    return null;
                }

                @Override
                public ItemStack getIcon(Object object, int count) {
                    return icon.getDefaultStack();
                }

                @Override
                public Text textify(Object object) {
                    return Text.translatable("text.graves.teleportation_cost." + TYPE_NAME.get(this));
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

        T convert(String object);
        ItemStack getIcon(T object, int count);
        Text textify(T object);

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
