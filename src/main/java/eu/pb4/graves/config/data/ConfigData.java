package eu.pb4.graves.config.data;

import eu.pb4.graves.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class ConfigData {
    public int CONFIG_VERSION_DONT_TOUCH_THIS = ConfigManager.VERSION;
    public String _comment = "Before changing anything, see https://github.com/Patbox/UniversalGraves#configuration";
    public String graveType = "player_head";
    public String lockedTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjdjYWI1NmM4MmNiODFiZGI5OTc5YTQ2NGJjOWQzYmEzZTY3MjJiYTEyMmNmNmM1Mjg3MzAxMGEyYjU5YWVmZSJ9fX0=";
    public String unlockedTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjdjYWI1NmM4MmNiODFiZGI5OTc5YTQ2NGJjOWQzYmEzZTY3MjJiYTEyMmNmNmM1Mjg3MzAxMGEyYjU5YWVmZSJ9fX0=";

    public boolean isProtected = true;
    public int protectionTime = 300;
    public boolean shouldBreak = true;
    public int breakAfter = 900;

    public boolean createGravesFromPvP = true;
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
    public String creationFailedPvPGraveMessage = "<red><lang:'text.graves.created_failed_pvp':'<gold>${position}':'<yellow>${world}'>";

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

        return list;
    }

    private static List<String> getDefaultHologram() {
        List<String> list = new ArrayList<>();

        list.add("<gold><lang:'text.graves.grave_of':'<white>${player}'>");
        list.add("<yellow>${death_cause}");
        list.add("");
        list.add("<gray><lang:'text.graves.items_xp':'<white>${item_count}':'<white>${xp}'>");
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
}
