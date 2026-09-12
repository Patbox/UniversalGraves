package eu.pb4.graves.other;

import eu.pb4.graves.grave.Grave;
import eu.pb4.graves.grave.PositionedItemStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public record DynamicLevelUnlockCost(
        int stackDivisor,
        int enchantmentDivisor,
        EnchantmentCostMode enchantmentCostMode,
        int minimumCost,
        double ownerMultiplier,
        double nonOwnerMultiplier
) implements GraveUnlockCost {
    public DynamicLevelUnlockCost {
        stackDivisor = Math.max(1, stackDivisor);
        enchantmentDivisor = Math.max(1, enchantmentDivisor);
        enchantmentCostMode = enchantmentCostMode != null ? enchantmentCostMode : EnchantmentCostMode.LEVELS;
        minimumCost = Math.max(0, minimumCost);
        ownerMultiplier = sanitizeMultiplier(ownerMultiplier);
        nonOwnerMultiplier = sanitizeMultiplier(nonOwnerMultiplier);
    }

    @Override
    public GenericCost<?> quote(Grave grave, boolean owner) {
        long equivalentStacks = this.countEquivalentStacks(grave.getItems());
        long enchantmentValue = this.countEnchantmentValue(grave.getItems());
        return new GenericCost<>(GenericCost.Type.LEVEL, null, this.calculateFinalCost(equivalentStacks, enchantmentValue, owner));
    }

    @Override
    public GenericCost<?> baseQuote(Grave grave) {
        long equivalentStacks = this.countEquivalentStacks(grave.getItems());
        long enchantmentValue = this.countEnchantmentValue(grave.getItems());
        return new GenericCost<>(GenericCost.Type.LEVEL, null, this.calculateBaseCost(equivalentStacks, enchantmentValue));
    }

    int calculateBaseCost(long equivalentStacks, long enchantmentValue) {
        long stackCost = equivalentStacks / this.stackDivisor;
        long enchantmentCost = enchantmentValue / this.enchantmentDivisor;
        long baseCost = Math.max(this.minimumCost, saturatedAdd(stackCost, enchantmentCost));
        return clampToInt(baseCost);
    }

    int calculateFinalCost(long equivalentStacks, long enchantmentValue, boolean owner) {
        int baseCost = this.calculateBaseCost(equivalentStacks, enchantmentValue);
        double multiplier = owner ? this.ownerMultiplier : this.nonOwnerMultiplier;
        return floorAndClamp(baseCost * multiplier);
    }

    @Override
    public boolean requiresPayment() {
        return true;
    }

    long countEquivalentStacks(Iterable<? extends PositionedItemStack> items) {
        return countEquivalentStacks(
                items,
                positionedStack -> positionedStack.stack().getCount(),
                positionedStack -> positionedStack.stack().getMaxStackSize(),
                (first, second) -> ItemStack.isSameItemSameComponents(first.stack(), second.stack())
        );
    }

    static <T> long countEquivalentStacks(Iterable<T> items, ToLongFunction<T> count, ToIntFunction<T> maxStackSize,
                                          BiPredicate<T, T> compatible) {
        List<StackGroup<T>> groups = new ArrayList<>();

        for (var item : items) {
            long itemCount = count.applyAsLong(item);
            if (itemCount <= 0) {
                continue;
            }

            StackGroup<T> matchingGroup = null;
            for (var group : groups) {
                if (compatible.test(group.representative, item)) {
                    matchingGroup = group;
                    break;
                }
            }

            if (matchingGroup == null) {
                groups.add(new StackGroup<>(item, itemCount));
            } else {
                matchingGroup.count = saturatedAdd(matchingGroup.count, itemCount);
            }
        }

        long result = 0;
        for (var group : groups) {
            int groupMaxStackSize = Math.max(1, maxStackSize.applyAsInt(group.representative));
            long equivalentCount = 1 + (group.count - 1) / groupMaxStackSize;
            result = saturatedAdd(result, equivalentCount);
        }
        return result;
    }

    long countEnchantmentValue(Iterable<? extends PositionedItemStack> items) {
        long result = 0;

        for (var positionedStack : items) {
            var stack = positionedStack.stack();
            if (stack.isEmpty()) {
                continue;
            }

            var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
            if (this.enchantmentCostMode == EnchantmentCostMode.ENCHANTED_STACKS) {
                if (!enchantments.isEmpty()) {
                    result = saturatedAdd(result, 1);
                }
            } else {
                for (var entry : enchantments.entrySet()) {
                    result = saturatedAdd(result, entry.getIntValue());
                }
            }
        }

        return result;
    }

    private static double sanitizeMultiplier(double multiplier) {
        return Double.isFinite(multiplier) ? Math.max(0, multiplier) : 0;
    }

    private static long saturatedAdd(long first, long second) {
        if (Long.MAX_VALUE - first < second) {
            return Long.MAX_VALUE;
        }
        return first + second;
    }

    private static int clampToInt(long value) {
        if (value <= 0) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }

    private static int floorAndClamp(double value) {
        if (Double.isNaN(value) || value <= 0) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.floor(value);
    }

    private static final class StackGroup<T> {
        private final T representative;
        private long count;

        private StackGroup(T representative, long count) {
            this.representative = representative;
            this.count = count;
        }
    }

    public enum EnchantmentCostMode {
        LEVELS,
        ENCHANTED_STACKS;

        public static EnchantmentCostMode fromConfig(String value) {
            if (value == null) {
                return LEVELS;
            }

            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return LEVELS;
            }
        }

        public String configName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
