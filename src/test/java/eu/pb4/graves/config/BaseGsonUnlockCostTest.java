package eu.pb4.graves.config;

import com.google.gson.JsonParser;
import eu.pb4.graves.other.DynamicLevelUnlockCost;
import eu.pb4.graves.other.GenericCost;
import eu.pb4.graves.other.GraveUnlockCost;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BaseGsonUnlockCostTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void roundTripsDynamicUnlockCost() {
        var gson = BaseGson.getGson(RegistryAccess.EMPTY);
        var input = """
                {
                  "type": "dynamic_level",
                  "stack_divisor": 3,
                  "enchantment_divisor": 4,
                  "enchantment_cost_mode": "enchanted_stacks",
                  "minimum_cost": 2,
                  "owner_multiplier": 1.25,
                  "non_owner_multiplier": 3.0
                }
                """;

        var parsed = assertInstanceOf(DynamicLevelUnlockCost.class, gson.fromJson(input, GraveUnlockCost.class));
        assertEquals(3, parsed.stackDivisor());
        assertEquals(DynamicLevelUnlockCost.EnchantmentCostMode.ENCHANTED_STACKS, parsed.enchantmentCostMode());
        assertEquals(JsonParser.parseString(input), JsonParser.parseString(gson.toJson(parsed, GraveUnlockCost.class)));
    }

    @Test
    void preservesLegacyStaticCostShape() {
        var gson = BaseGson.getGson(RegistryAccess.EMPTY);
        var input = "{\"type\":\"level\",\"count\":7}";

        var parsed = assertInstanceOf(GraveUnlockCost.Static.class, gson.fromJson(input, GraveUnlockCost.class));
        assertEquals(GenericCost.Type.LEVEL, parsed.cost().type());
        assertEquals(7, parsed.cost().count());
        assertEquals(JsonParser.parseString(input), JsonParser.parseString(gson.toJson(parsed, GraveUnlockCost.class)));
    }
}
