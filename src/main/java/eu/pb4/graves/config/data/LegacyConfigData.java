package eu.pb4.graves.config.data;

import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.graves.config.Config;
import eu.pb4.graves.config.ConfigManager;
import eu.pb4.graves.other.GravesXPCalculation;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.util.Identifier;

import java.util.*;

@Deprecated
public class LegacyConfigData {
    public static final String LEGACY_VERSION_KEY = "CONFIG_VERSION_DONT_TOUCH_THIS";

    public int protectionTime = 900;
    public int breakingTime = 1800;
    public boolean keepBlockAfterBreaking = false;
    public boolean restoreBlockAfterPlayerBreaking = true;
    public int maxGraveCount = -1;

    public String xpStorageType = GravesXPCalculation.PERCENT_POINTS.name;
    public double xpPercentTypeValue = 100;

    public boolean replaceAnyBlock = false;
    public int maxPlacementDistance = 8;
    public boolean shiftLocationOnFailure = true;
    public int maxShiftCount = 5;
    public int shiftDistance = 40;
    public boolean useRealTime = false;
    public boolean useAlternativeXPEntity = FabricLoader.getInstance().isModLoaded("origins")
            || FabricLoader.getInstance().isModLoaded("bewitchment");

    public boolean createFromPvP = true;
    public boolean createFromVoid = true;
    public boolean createFromCommandDeaths = true;
    public Map<String, Boolean> createInProtectedArea = new HashMap<>();
    public boolean dropItemsAfterExpiring = true;

    public boolean allowAttackersToTakeItems = false;
    public boolean shiftClickTakesItems = true;
    public boolean giveGraveCompass = true;
    public boolean allowRemoteProtectionRemoval = true;
    public boolean allowRemoteGraveBreaking = true;

    public boolean clickGraveToOpenGui = true;

    public List<String> blacklistedWorlds = new ArrayList<>();
    public Map<String, List<Config.Arena>> blacklistedAreas = new HashMap<>();
    public Set<String> blacklistedDamageSources = new HashSet<>();

    public List<String> skippedEnchantments = new ArrayList<>();


    public Config convert() {
        var config = new Config();
        config.protection.allowAttackersToTakeItems = this.allowAttackersToTakeItems;
        config.protection.breakingTime = this.breakingTime;
        config.protection.useRealTime = this.useRealTime;
        config.protection.protectionTime = this.protectionTime;
        config.protection.dropItemsAfterExpiring = this.dropItemsAfterExpiring;
        config.storage.xpStorageType = GravesXPCalculation.byName(this.xpStorageType);
        config.storage.xpPercentTypeValue = this.xpPercentTypeValue;
        config.storage.useAlternativeXPEntity = this.useAlternativeXPEntity;
        config.placement.keepBlockAfterBreaking = this.keepBlockAfterBreaking;
        config.placement.restoreBlockAfterPlayerBreaking = this.restoreBlockAfterPlayerBreaking;
        config.placement.maxGraveCount = this.maxGraveCount;
        config.placement.replaceAnyBlock = this.replaceAnyBlock;
        config.placement.maxPlacementDistance = this.maxPlacementDistance;
        config.placement.shiftLocationOnFailure = this.shiftLocationOnFailure;
        config.placement.maxShiftCount = this.maxShiftCount;
        config.placement.shiftDistance = this.shiftDistance;

        config.interactions.allowRemoteGraveBreaking = this.allowRemoteGraveBreaking;
        config.interactions.clickGraveToOpenGui = this.clickGraveToOpenGui;
        config.interactions.shiftClickTakesItems = this.shiftClickTakesItems;
        config.interactions.giveGraveCompass = this.giveGraveCompass;
        config.interactions.allowRemoteProtectionRemoval = this.allowRemoteProtectionRemoval;

        for (var entry : this.blacklistedAreas.entrySet()) {
            config.placement.blacklistedAreas.put(Identifier.tryParse(entry.getKey()), entry.getValue());
        }

        for (var entry : this.blacklistedDamageSources) {
            config.placement.ignoredDamageTypes.put(Identifier.tryParse(entry), WrappedText.EMPTY);
        }

        for (var entry : this.skippedEnchantments) {
            config.storage.skippedEnchantments.add(Identifier.tryParse(entry));
        }
        for (var entry : this.blacklistedWorlds) {
            config.placement.blacklistedWorlds.add(Identifier.tryParse(entry));
        }

        for (var entry : this.createInProtectedArea.entrySet()) {
            config.placement.blockInProtection.put(Identifier.tryParse(entry.getKey()), entry.getValue());
        }

        if (!this.createFromPvP) {
            config.placement.ignoredAttackers.put(EntityType.PLAYER, WrappedText.EMPTY);
        }

        if (!this.createFromVoid) {
            config.placement.ignoredDamageTypes.put(DamageTypes.OUT_OF_WORLD.getValue(), WrappedText.EMPTY);
        }

        if (!this.createFromCommandDeaths) {
            config.placement.ignoredDamageTypes.put(DamageTypes.GENERIC_KILL.getValue(), WrappedText.EMPTY);
        }


        return config;
    }
}
