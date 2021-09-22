package eu.pb4.graves.grave;

import eu.pb4.graves.config.ConfigManager;
import net.minecraft.server.network.ServerPlayerEntity;

public enum GravesXPCalculation {
    NONE("none", (p) -> 0),
    VANILLA("vanilla", (p) -> Math.min(p.experienceLevel * 7, 100)),
    DROP("drop", (p) -> Math.min(p.experienceLevel * 7, 100)),
    PERCENT_POINTS("percent_points", (p) -> {
        int points = 0;

        for (int i = 0; i < p.experienceLevel; i++) {
            if (i >= 30) {
                points += 112 + (i - 30) * 9;
            } else {
                points += i >= 15 ? 37 + (i - 15) * 5 : 7 + i * 2;
            }
        }

        points += p.experienceProgress * p.getNextLevelExperience();
        return (int) (points * ConfigManager.getConfig().configData.xpPercentTypeValue / 100);
    }),

    PERCENT_LEVELS("percent_levels", (p) -> {
        int points = 0;
        double percent = ConfigManager.getConfig().configData.xpPercentTypeValue / 100;

        for (int i = 0; i < p.experienceLevel * percent; i++) {
            if (i >= 30) {
                points += 112 + (i - 30) * 9;
            } else {
                points += i >= 15 ? 37 + (i - 15) * 5 : 7 + i * 2;
            }
        }

        points += p.experienceProgress * p.getNextLevelExperience() * percent;
        return points;
    });

    public final String name;
    public final Player2XP converter;

    GravesXPCalculation(String name, Player2XP converter) {
        this.name = name;
        this.converter = converter;
    }

    public static GravesXPCalculation byName(String name) {
        for (GravesXPCalculation type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return GravesXPCalculation.NONE;
    }

    public static String next(String xpStorageType) {
        var byName = byName(xpStorageType);

        return GravesXPCalculation.values()[(byName.ordinal() + 1) % GravesXPCalculation.values().length].name;
    }

    @FunctionalInterface
    public interface Player2XP {
        int calc(ServerPlayerEntity player);
    }

}
