package eu.pb4.graves.registry;

import eu.pb4.graves.config.ConfigManager;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;

public class GraveGameRules {
    public static final GameRules.Key<GameRules.IntRule> PROTECTION_TIME =
            GameRuleRegistry.register("universal_graves:protection_time", GameRules.Category.PLAYER, GameRuleFactory.createIntRule(-2, -2));
    public static final GameRules.Key<GameRules.IntRule> BREAKING_TIME =
            GameRuleRegistry.register("universal_graves:breaking_time", GameRules.Category.PLAYER, GameRuleFactory.createIntRule(-2, -2));


    public static final int getProtectionTime(MinecraftServer server) {
        var rule = server.getOverworld().getGameRules().get(PROTECTION_TIME).get();

        if (rule == -2) {
            return ConfigManager.getConfig().protection.protectionTime;
        } else {
            return rule;
        }
    }

    public static final int getBreakingTime(MinecraftServer server) {
        var rule = server.getOverworld().getGameRules().get(BREAKING_TIME).get();

        if (rule == -2) {
            return ConfigManager.getConfig().protection.breakingTime;
        } else {
            return rule;
        }
    }
}
