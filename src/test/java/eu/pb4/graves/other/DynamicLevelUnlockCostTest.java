package eu.pb4.graves.other;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicLevelUnlockCostTest {
    private static final DynamicLevelUnlockCost COST = new DynamicLevelUnlockCost(
            3, 4, DynamicLevelUnlockCost.EnchantmentCostMode.LEVELS, 1, 1, 1.5
    );

    @Test
    void combinesCompatibleStacksUsingTheirMaximumStackSize() {
        assertEquals(1, count(new Stack("cobblestone", "", 32, 64), new Stack("cobblestone", "", 20, 64)));
        assertEquals(2, count(new Stack("cobblestone", "", 64, 64), new Stack("cobblestone", "", 6, 64)));
        assertEquals(2, count(new Stack("ender_pearl", "", 16, 16), new Stack("ender_pearl", "", 5, 16)));
        assertEquals(2, count(new Stack("iron_pickaxe", "", 1, 1), new Stack("iron_pickaxe", "", 1, 1)));
    }

    @Test
    void keepsStacksWithDifferentComponentsSeparate() {
        assertEquals(2, count(new Stack("cobblestone", "", 32, 64), new Stack("cobblestone", "named", 20, 64)));
    }

    @Test
    void appliesDivisorsMinimumAndFloorMultiplier() {
        assertEquals(4, COST.calculateBaseCost(6, 8));
        assertEquals(4, COST.calculateFinalCost(6, 8, true));
        assertEquals(6, COST.calculateFinalCost(6, 8, false));
        assertEquals(1, COST.calculateFinalCost(0, 0, true));
        assertEquals(1, COST.calculateFinalCost(0, 0, false));
    }

    @Test
    void sanitizesInvalidConfigurationValues() {
        var cost = new DynamicLevelUnlockCost(0, -2, null, -1, -4, Double.NaN);

        assertEquals(1, cost.stackDivisor());
        assertEquals(1, cost.enchantmentDivisor());
        assertEquals(DynamicLevelUnlockCost.EnchantmentCostMode.LEVELS, cost.enchantmentCostMode());
        assertEquals(0, cost.minimumCost());
        assertEquals(0, cost.ownerMultiplier());
        assertEquals(0, cost.nonOwnerMultiplier());
    }

    @Test
    void parsesEnchantmentModesAndFallsBackToLevels() {
        assertEquals(DynamicLevelUnlockCost.EnchantmentCostMode.ENCHANTED_STACKS,
                DynamicLevelUnlockCost.EnchantmentCostMode.fromConfig("enchanted_stacks"));
        assertEquals(DynamicLevelUnlockCost.EnchantmentCostMode.LEVELS,
                DynamicLevelUnlockCost.EnchantmentCostMode.fromConfig("unknown"));
    }

    @Test
    void clampsOverflowToTheMaximumSupportedLevelCost() {
        assertEquals(Integer.MAX_VALUE, COST.calculateBaseCost(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, COST.calculateFinalCost(Long.MAX_VALUE, Long.MAX_VALUE, false));
    }

    private static long count(Stack... stacks) {
        return DynamicLevelUnlockCost.countEquivalentStacks(
                List.of(stacks), Stack::count, Stack::maxStackSize,
                (first, second) -> first.item.equals(second.item) && first.components.equals(second.components)
        );
    }

    private record Stack(String item, String components, int count, int maxStackSize) {}
}
