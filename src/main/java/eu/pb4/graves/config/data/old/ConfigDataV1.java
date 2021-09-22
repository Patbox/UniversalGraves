package eu.pb4.graves.config.data.old;

import eu.pb4.graves.config.data.ConfigData;
import eu.pb4.graves.config.data.VersionedConfigData;
import eu.pb4.graves.grave.GravesLookType;
import eu.pb4.graves.grave.GravesXPCalculation;

import java.util.ArrayList;
import java.util.List;

public class ConfigDataV1 extends VersionedConfigData {
    public String graveType = GravesLookType.PLAYER_HEAD.name;
    public String lockedTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjdjYWI1NmM4MmNiODFiZGI5OTc5YTQ2NGJjOWQzYmEzZTY3MjJiYTEyMmNmNmM1Mjg3MzAxMGEyYjU5YWVmZSJ9fX0=";
    public String unlockedTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjdjYWI1NmM4MmNiODFiZGI5OTc5YTQ2NGJjOWQzYmEzZTY3MjJiYTEyMmNmNmM1Mjg3MzAxMGEyYjU5YWVmZSJ9fX0=";

    public boolean isProtected = true;
    public boolean shouldProtectionExpire = true;
    public int protectionTime = 300;
    public boolean shouldBreak = true;
    public int breakAfter = 900;

    public boolean storeExperience = true;
    public String xpStorageType = GravesXPCalculation.VANILLA.name;
    public double xpPercentTypeValue = 100;

    public boolean createGravesFromPvP = true;
    public boolean createGravesInClaims = true;
    public boolean dropItemsAfterExpiring = true;

    public boolean hologram = true;
    public List<String> hologramProtectedText = getDefaultProtectedHologram();
    public List<String> hologramText = getDefaultHologram();

    public String guiTitle = "<lang:'text.graves.gui_title':'${player}'>";
    public List<String> guiProtectedText = getDefaultProtectedGui();
    public List<String> guiText = getDefaultGui();

    public boolean displayNoLongerProtectedMessage = true;
    public String noLongerProtectedMessage = "<red><lang:'text.graves.no_longer_protected':'<gold>${position}':'<white>${world}':'<yellow>${item_count}'>";

    public boolean displayGraveExpiredMessage = true;
    public String graveExpiredMessage = "<red><lang:'text.graves.expired':'<gold>${position}':'<white>${world}':'<yellow>${item_count}'>";

    public boolean displayGraveBrokenMessage = true;
    public String graveBrokenMessage = "<gray><lang:'text.graves.somebody_broke':'<white>${position}':'<white>${world}':'<white>${item_count}'>";

    public boolean displayCreatedGraveMessage = true;
    public String createdGraveMessage = "<white><lang:'text.graves.created_at':'<yellow>${position}':'<gray>${world}'>";

    public boolean displayCreationFailedGraveMessage = true;
    public String creationFailedGraveMessage = "<red><lang:'text.graves.creation_failed':'<gold>${position}':'<yellow>${world}'>";

    public boolean displayCreationFailedPvPGraveMessage = true;
    public String creationFailedPvPGraveMessage = "<red><lang:'text.graves.creation_failed_pvp':'<gold>${position}':'<yellow>${world}'>";

    public boolean displayCreationFailedClaimGraveMessage = true;
    public String creationFailedClaimMessage = "<red><lang:'text.graves.creation_failed_claim':'<gold>${position}':'<yellow>${world}'>";

    public String neverExpires = "Never";

    public String yearsText = "y";
    public String daysText = "d";
    public String hoursText = "h";
    public String minutesText = "m";
    public String secondsText = "s";

    public String graveTitle = "<lang:'text.graves.players_grave':'${player}'>";


    private static List<String> getDefaultProtectedHologram() {
        List<String> list = new ArrayList<>();

        list.add("<gold><lang:'text.graves.grave_of':'<white>${player}'>");
        list.add("<yellow>${death_cause}");
        list.add("");
        list.add("<gray><lang:'text.graves.items_xp':'<white>${item_count}':'<white>${xp}'>");
        list.add("<blue><lang:'text.graves.protected_time':'<white>${protection_time}'>");
        list.add("<red><lang:'text.graves.break_time':'<white>${break_time}'>");

        return list;
    }

    private static List<String> getDefaultHologram() {
        List<String> list = new ArrayList<>();

        list.add("<gold><lang:'text.graves.grave_of':'<white>${player}'>");
        list.add("<yellow>${death_cause}");
        list.add("");
        list.add("<gray><lang:'text.graves.items_xp':'<white>${item_count}':'<white>${xp}'>");
        list.add("<blue><lang:'text.graves.not_protected'>");
        list.add("<red><lang:'text.graves.break_time':'<white>${break_time}'>");

        return list;
    }

    private static List<String> getDefaultProtectedGui() {
        List<String> list = new ArrayList<>();

        list.add("${position} <gray>(${world})");
        list.add("<yellow>${death_cause}");
        list.add("<gray><lang:'text.graves.items_xp':'<white>${item_count}':'<white>${xp}'>");
        list.add("<blue><lang:'text.graves.protected_time':'<white>${protection_time}'>");
        list.add("<red><lang:'text.graves.break_time':'<white>${break_time}'>");

        return list;
    }

    private static List<String> getDefaultGui() {
        List<String> list = new ArrayList<>();

        list.add("${position} <gray>(${world})");
        list.add("<yellow>${death_cause}");
        list.add("<gray><lang:'text.graves.items_xp':'<white>${item_count}':'<white>${xp}'>");
        list.add("<blue><lang:'text.graves.not_protected'>");
        list.add("<red><lang:'text.graves.break_time':'<white>${break_time}'>");

        return list;
    }

    public ConfigData update() {
        var config = new ConfigData();
        config.graveStyle = this.graveType;
        config.presetHeadUnlockedTexture = this.unlockedTexture;
        config.presetHeadLockedTexture = this.lockedTexture;
        config.isProtected = this.isProtected;
        config.protectionTime = this.shouldProtectionExpire ? this.protectionTime : -1;
        config.breakingTime = this.shouldBreak ? this.breakAfter : -1;
        config.xpStorageType = this.storeExperience ? this.xpStorageType : GravesXPCalculation.DROP.name;
        config.xpPercentTypeValue = this.xpPercentTypeValue;
        config.createFromPvP = this.createGravesFromPvP;
        config.createInClaims = this.createGravesInClaims;
        config.dropItemsAfterExpiring = this.dropItemsAfterExpiring;
        config.hologram = this.hologram;
        config.hologramProtectedText = this.hologramProtectedText;
        config.hologramText = this.hologramText;
        config.guiTitle = this.guiTitle;
        config.guiProtectedText = this.guiProtectedText;
        config.guiText = this.guiText;
        config.messageProtectionEnded = this.displayNoLongerProtectedMessage ? this.noLongerProtectedMessage : "";
        config.messageCreationFailed = this.displayCreationFailedGraveMessage ? this.creationFailedGraveMessage : "";
        config.messageGraveCreated = this.displayCreatedGraveMessage ? this.createdGraveMessage : "";
        config.messageGraveExpired = this.displayGraveExpiredMessage ? this.graveExpiredMessage : "";
        config.messageGraveBroken = this.displayGraveBrokenMessage ? this.graveBrokenMessage : "";
        config.messageCreationFailed = this.displayCreationFailedGraveMessage ? this.creationFailedGraveMessage : "";
        config.messageCreationFailedPvP = this.displayCreationFailedPvPGraveMessage ? this.creationFailedPvPGraveMessage : "";
        config.messageCreationFailedClaim = this.displayCreationFailedClaimGraveMessage ? this.creationFailedClaimMessage : "";
        config.neverExpires = this.neverExpires;
        config.yearsText = this.yearsText;
        config.daysText = this.daysText;
        config.hoursText = this.hoursText;
        config.minutesText = this.minutesText;
        config.secondsText = this.secondsText;

        config.graveTitle = this.graveTitle;
        return config;
    }
}