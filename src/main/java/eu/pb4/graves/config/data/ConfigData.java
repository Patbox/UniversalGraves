package eu.pb4.graves.config.data;

import eu.pb4.graves.other.GravesLookType;
import eu.pb4.graves.other.GravesXPCalculation;
import eu.pb4.graves.other.GraveUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.WallShape;

import java.util.*;

public class ConfigData extends VersionedConfigData {
    public String _comment = "Before changing anything, see https://github.com/Patbox/UniversalGraves#configuration";

    public String graveStyle = GravesLookType.CLIENT_MODEL_OR_HEAD.name;
    public boolean allowClientSideStyle = true;
    public boolean playerHeadTurnIntoSkulls = true;
    public String presetHeadLockedTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjdjYWI1NmM4MmNiODFiZGI5OTc5YTQ2NGJjOWQzYmEzZTY3MjJiYTEyMmNmNmM1Mjg3MzAxMGEyYjU5YWVmZSJ9fX0=";
    public String presetHeadUnlockedTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjdjYWI1NmM4MmNiODFiZGI5OTc5YTQ2NGJjOWQzYmEzZTY3MjJiYTEyMmNmNmM1Mjg3MzAxMGEyYjU5YWVmZSJ9fX0=";
    public List<String> customBlockStateLockedStyles = List.of(
            GraveUtils.blockStateToString(Blocks.STONE_BRICK_WALL.getDefaultState().with(WallBlock.NORTH_SHAPE, WallShape.LOW).with(WallBlock.SOUTH_SHAPE, WallShape.LOW).with(WallBlock.UP, true)),
            GraveUtils.blockStateToString(Blocks.STONE_BRICK_WALL.getDefaultState().with(WallBlock.EAST_SHAPE, WallShape.LOW).with(WallBlock.WEST_SHAPE, WallShape.LOW).with(WallBlock.UP, true))
    );

    public List<String> customBlockStateUnlockedStyles = List.of(
            GraveUtils.blockStateToString(Blocks.MOSSY_STONE_BRICK_WALL.getDefaultState().with(WallBlock.NORTH_SHAPE, WallShape.LOW).with(WallBlock.SOUTH_SHAPE, WallShape.LOW).with(WallBlock.UP, true)),
            GraveUtils.blockStateToString(Blocks.MOSSY_STONE_BRICK_WALL.getDefaultState().with(WallBlock.EAST_SHAPE, WallShape.LOW).with(WallBlock.WEST_SHAPE, WallShape.LOW).with(WallBlock.UP, true))
    );

    public List<String> customStyleSignText = getDefaultSign();

    public List<String> customStyleSignProtectedText = getDefaultProtectedSign();

    public List<String> customStyleSignVisualText = getDefaultVisualSign();

    public int customStyleUpdateRate = 20;

    public int protectionTime = 900;
    public int breakingTime = 1800;
    public boolean keepBlockAfterBreaking = false;
    public boolean restoreBlockAfterPlayerBreaking = true;

    public String xpStorageType = GravesXPCalculation.PERCENT_POINTS.name;
    public double xpPercentTypeValue = 100;

    public boolean replaceAnyBlock = false;
    public int maxPlacementDistance = 8;
    public boolean useRealTime = false;
    public boolean useAlternativeXPEntity = FabricLoader.getInstance().isModLoaded("origins")
            || FabricLoader.getInstance().isModLoaded("bewitchment");

    public boolean createFromPvP = true;
    public boolean createFromVoid = true;
    public boolean createFromCommandDeaths = true;
    public boolean createInClaims = true;
    public boolean dropItemsAfterExpiring = true;

    public boolean allowAttackersToTakeItems = false;
    public boolean shiftClickTakesItems = true;
    public boolean giveGraveCompass = true;
    public boolean allowRemoteProtectionRemoval = true;
    public boolean allowRemoteGraveBreaking = true;

    public String graveTitle = "<lang:'text.graves.players_grave':'${player}'>";

    public boolean hologram = true;
    public boolean hologramDisplayIfOnClient = false;
    public double hologramOffset = 1.2;
    public List<String> hologramProtectedText = getDefaultProtectedHologram();
    public List<String> hologramText = getDefaultHologram();
    public List<String> hologramVisualText = getDefaultVisualHologram();

    public boolean clickGraveToOpenGui = true;
    public String guiTitle = "<lang:'text.graves.gui_title':'${player}'>";
    public List<String> guiProtectedText = getDefaultProtectedGui();
    public List<String> guiText = getDefaultGui();

    public List<String> guiProtectedItem = Collections.singletonList("chest");
    public List<String> guiItem = Collections.singletonList("trapped_chest");

    public String messageGraveCreated = "<white><lang:'text.graves.created_at_expire':'<yellow>${position}':'<gray>${world}':'<red>${break_time}'>";
    public String messageProtectionEnded = "<red><lang:'text.graves.no_longer_protected':'<gold>${position}':'<white>${world}':'<yellow>${item_count}'>";
    public String messageGraveExpired = "<red><lang:'text.graves.expired':'<gold>${position}':'<white>${world}':'<yellow>${item_count}'>";
    public String messageGraveBroken = "<gray><lang:'text.graves.somebody_broke':'<white>${position}':'<white>${world}':'<white>${item_count}'>";
    public String messageCreationFailed = "<red><lang:'text.graves.creation_failed':'<gold>${position}':'<yellow>${world}'>";
    public String messageCreationFailedVoid = "<red><lang:'text.graves.creation_failed_void':'<gold>${position}':'<yellow>${world}'>";
    public String messageCreationFailedPvP = "<red><lang:'text.graves.creation_failed_pvp':'<gold>${position}':'<yellow>${world}'>";
    public String messageCreationFailedClaim = "<red><lang:'text.graves.creation_failed_claim':'<gold>${position}':'<yellow>${world}'>";

    public String yearsText = "y";
    public String daysText = "d";
    public String hoursText = "h";
    public String minutesText = "m";
    public String secondsText = "s";

    public String infinityText = "∞";

    public String fullDateFormat = "dd.MM.yyyy, HH:mm";

    public Map<String, String> worldNameOverrides = new HashMap<>();
    public List<String> blacklistedWorlds = new ArrayList<>();
    public Map<String, List<Arena>> blacklistedAreas = new HashMap<>();
    public Set<String> blacklistedDamageSources = new HashSet<>();

    @Deprecated
    public boolean tryDetectionSoulbound = false;

    public List<String> skippedEnchantments = new ArrayList<>();

    public String guiPreviousPageText = "<lang:'text.graves.gui.previous_page'>";
    public String guiPreviousPageBlockedText = "<dark_gray><lang:'text.graves.gui.previous_page'>";

    public String guiNextPageText = "<lang:'text.graves.gui.next_page'>";
    public String guiNextPageBlockedText = "<dark_gray><lang:'text.graves.gui.next_page'>";

    public String guiRemoveProtectionText = "<red><lang:'text.graves.gui.remove_protection'>";
    public String guiBreakGraveText = "<red><lang:'text.graves.gui.break_grave'>";

    public String guiQuickPickupText = "<yellow><lang:'text.graves.gui.quick_pickup'>";
    public String guiCantReverseAction = "<dark_red><bold><lang:'text.graves.gui.cant_reverse'>";
    public String guiClickToConfirm = "<white><lang:'text.graves.gui.click_to_confirm'>";


    public String guiInfoIcon = "minecraft:oak_sign";
    public String guiBarItem = "minecraft:white_stained_glass_pane";

    public String guiPreviousPageIcon = "universal_graves:icon{Texture:\"previous_page\"}";
    public String guiPreviousPageBlockedIcon = "universal_graves:icon{Texture:\"previous_page_blocked\"}";

    public String guiNextPageIcon = "universal_graves:icon{Texture:\"next_page\"}";
    public String guiNextPageBlockedIcon = "universal_graves:icon{Texture:\"next_page_blocked\"}";

    public String guiRemoveProtectionIcon = "universal_graves:icon{Texture:\"remove_protection\"}";
    public String guiBreakGraveIcon = "universal_graves:icon{Texture:\"break_grave\"}";

    public String guiQuickPickupIcon = "universal_graves:icon{Texture:\"quick_pickup\"}";

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

    private static List<String> getDefaultVisualHologram() {
        List<String> list = new ArrayList<>();

        list.add("<gold><lang:'text.graves.grave_of':'<white>${player}'>");
        list.add("<yellow>${death_cause}");

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

    private static List<String> getDefaultProtectedSign() {
        List<String> list = new ArrayList<>();

        list.add("<white>${player}");
        list.add("<gray><lang:'text.graves.items_xp':'<white>${item_count}':'<white>${xp}'>");
        list.add("<blue><lang:'text.graves.protected_time_sign'>");
        list.add("<white>${protection_time}");

        return list;
    }

    private List<String> getDefaultVisualSign() {
        List<String> list = new ArrayList<>();

        list.add("<white>${player}");

        return list;
    }

    private static List<String> getDefaultSign() {
        List<String> list = new ArrayList<>();

        list.add("<white>${player}");
        list.add("<gray><lang:'text.graves.items_xp':'<white>${item_count}':'<white>${xp}'>");
        list.add("<red><lang:'text.graves.break_time_sign'>");
        list.add("<white>${break_time}");

        return list;
    }

    public class Arena {
        public int x1;
        public int y1;
        public int z1;
        public int x2;
        public int y2;
        public int z2;
    }
}
