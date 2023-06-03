package eu.pb4.graves.config;

import com.google.gson.annotations.SerializedName;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.graves.config.data.IconData;
import eu.pb4.graves.config.data.Variant;
import eu.pb4.graves.config.data.WrappedDateFormat;
import eu.pb4.graves.config.data.WrappedText;
import eu.pb4.graves.other.GravesXPCalculation;
import eu.pb4.graves.other.TeleportationCost;
import eu.pb4.graves.registry.IconItem;
import eu.pb4.predicate.api.BuiltinPredicates;
import eu.pb4.predicate.api.MinecraftPredicate;
import eu.pb4.predicate.api.PredicateContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.*;

public class Config {
    public String _comment = "Before changing anything, see https://github.com/Patbox/UniversalGraves#configuration";

    @SerializedName("config_version")
    public int version = ConfigManager.VERSION;

    @SerializedName("protection")
    public Protection protection = new Protection();

    public String getGraveModel(ServerPlayerEntity player) {
        var context = PredicateContext.of(player);
        for (var model : this.model.alternative) {
            if (model.predicate.test(context).success()) {
                return model.model;
            }
        }

        return this.model.defaultModelId;
    }

    public class Protection {
        @SerializedName("non_owner_protection_time")
        public int protectionTime = 900;
        @SerializedName("self_destruction_time")
        public int breakingTime = 1800;
        @SerializedName("drop_items_on_expiration")
        public boolean dropItemsAfterExpiring = true;
        @SerializedName("attackers_bypass_protection")
        public boolean allowAttackersToTakeItems = false;
        @SerializedName("use_real_time")
        public boolean useRealTime = false;
    }

    @SerializedName("interactions")
    public Interactions interactions = new Interactions();

    public static class Interactions {
        @SerializedName("give_death_compass")
        public boolean giveGraveCompass = true;
        @SerializedName("enable_click_to_open_gui")
        public boolean clickGraveToOpenGui = true;
        @SerializedName("shift_and_use_quick_pickup")
        public boolean shiftClickTakesItems = true;
        @SerializedName("allow_remote_protection_removal")
        public boolean allowRemoteProtectionRemoval = true;
        @SerializedName("allow_remote_breaking")
        public boolean allowRemoteGraveBreaking = true;
    }

    @SerializedName("storage")
    public Storage storage = new Storage();

    public class Storage {
        @SerializedName("experience_type")
        public GravesXPCalculation xpStorageType = GravesXPCalculation.PERCENT_POINTS;
        @SerializedName("experience_percent:setting_value")
        public double xpPercentTypeValue = 100;
        @SerializedName("alternative_experience_entity")
        public boolean useAlternativeXPEntity = FabricLoader.getInstance().isModLoaded("origins")
                || FabricLoader.getInstance().isModLoaded("bewitchment");
        @SerializedName("blocked_enchantments")
        public Set<Identifier> skippedEnchantments = new HashSet<>();
    }

    @SerializedName("placement")
    public Placement placement = new Placement();

    public class Placement {
        @SerializedName("player_grave_limit")
        public int maxGraveCount = -1;

        @SerializedName("replace_any_block")
        public boolean replaceAnyBlock = false;
        @SerializedName("max_distance_from_source_location")
        public int maxPlacementDistance = 8;
        @SerializedName("shift_location_on_failure")
        public boolean shiftLocationOnFailure = true;
        @SerializedName("max_shift_tries")
        public int maxShiftCount = 5;
        @SerializedName("max_shift_distance")
        public int shiftDistance = 40;

        @SerializedName("restore_replaced_block")
        public boolean keepBlockAfterBreaking = false;
        @SerializedName("restore_replaced_block_after_player_breaking")
        public boolean restoreBlockAfterPlayerBreaking = true;

        @SerializedName("cancel_creation_for_damage_types")
        public HashMap<Identifier, WrappedText> ignoredDamageTypes = new HashMap<>();
        @SerializedName("cancel_creation_for_ignored_attacker_types")
        public HashMap<EntityType<?>, WrappedText> ignoredAttackers = new HashMap<>();
        @SerializedName("block_in_protected_area")
        public Map<Identifier, Boolean> blockInProtection = new HashMap<>();
        @SerializedName("blacklisted_worlds")
        public Set<Identifier> blacklistedWorlds = new HashSet<>();

        @SerializedName("blacklisted_areas")
        public Map<Identifier, List<Arena>> blacklistedAreas = new HashMap<>();

        @SerializedName("creation_default_failure_text")
        public WrappedText messageCreationFailed = ofText("<red><lang:'text.graves.creation_failed':'<gold>${position}':'<yellow>${world}'>");
        @SerializedName("creation_claim_failure_text")
        public WrappedText messageCreationFailedClaim = ofText("<red><lang:'text.graves.creation_failed_claim':'<gold>${position}':'<yellow>${world}'>");
    }


    @SerializedName("teleportation")
    public Teleportation teleportation = new Teleportation();
    public class Teleportation {
        @SerializedName("cost")
        public TeleportationCost<?> teleportationCostType = new TeleportationCost<>(TeleportationCost.Type.CREATIVE, null, -1);
        @SerializedName("required_time")
        public int teleportTime = 5;
        @SerializedName("y_offset")
        public double teleportHeight = 1;

        @SerializedName("invincibility_time")
        public int invincibleTime = 2;

        @SerializedName("allow_movement_while_waiting")
        public boolean allowMovingDuringTeleportation = false;

        @SerializedName("text")
        public Texts text = new Texts();

        public class Texts {
            @SerializedName("timer")
            public WrappedText teleportTimerText = ofText("<lang:'text.graves.teleport.teleport_timer':'${time}'>");
            @SerializedName("timer_allow_moving")
            public WrappedText teleportTimerTextAllowMoving = ofText("<lang:'text.graves.teleport.teleport_timer_moving':'${time}'>");
            @SerializedName("location")
            public WrappedText teleportLocationText = ofText("<lang:'text.graves.teleport.teleport_location':'${position}'>");
            @SerializedName("canceled")
            public WrappedText teleportCancelledText = ofText("<red><lang:'text.graves.teleport.teleport_cancelled'>");
        }
    }

    @SerializedName("model")
    public Model model = new Model();
    public class Model {
        @SerializedName("default")
        public String defaultModelId = "default";
        @SerializedName("alternative")
        public List<CheckedModel> alternative = new ArrayList<>();

        public class CheckedModel {
            @SerializedName("require")
            public MinecraftPredicate predicate = BuiltinPredicates.operatorLevel(0);
            @SerializedName("model")
            public String model = "default";
        }
    }

    @SerializedName("ui")
    public Ui ui = new Ui();

    public static class Ui {
        @SerializedName("title")
        public WrappedText graveTitle = ofText("<lang:'text.graves.players_grave':'${player}'>");

        @SerializedName("list_grave_icon")
        public Variant<IconData> listGraveIcon = Variant.of(IconData.of(Items.CHEST, getDefaultProtectedGui()), IconData.of(Items.TRAPPED_CHEST, getDefaultGui()));

        @SerializedName("grave_info")
        public Variant<IconData> graveInfoIcon = Variant.of(IconData.of(Items.OAK_SIGN, getDefaultProtectedGui()), IconData.of(Items.OAK_SIGN, getDefaultGui()));

        @SerializedName("previous_button")
        public Variant<IconData> previousButton = Variant.of(
                IconData.of(IconItem.Texture.PREVIOUS_PAGE, "<lang:'text.graves.gui.previous_page'>"),
                IconData.of(IconItem.Texture.PREVIOUS_PAGE_BLOCKED, "<dark_gray><lang:'text.graves.gui.previous_page'>")
        );

        @SerializedName("next_button")
        public Variant<IconData> nextButton = Variant.of(
                IconData.of(IconItem.Texture.NEXT_PAGE, "<lang:'text.graves.gui.next_page'>"),
                IconData.of(IconItem.Texture.NEXT_PAGE_BLOCKED, "<dark_gray><lang:'text.graves.gui.next_page'>")
        );


        @SerializedName("remove_protection_button")
        public Variant<IconData> removeProtectionButton = Variant.of(
                IconData.of(IconItem.Texture.REMOVE_PROTECTION, "<red><lang:'text.graves.gui.remove_protection'>"),
                IconData.of(IconItem.Texture.REMOVE_PROTECTION, List.of(
                                "<red><lang:'text.graves.gui.remove_protection'>",
                                "<dark_red><bold><lang:'text.graves.gui.cant_reverse'>",
                                "",
                                "<white><lang:'text.graves.gui.click_to_confirm'>"
                        )
                )
        );

        @SerializedName("break_grave_button")
        public Variant<IconData> breakGraveButton = Variant.of(
                IconData.of(IconItem.Texture.BREAK_GRAVE, "<red><lang:'text.graves.gui.break_grave'>"),
                IconData.of(IconItem.Texture.BREAK_GRAVE, List.of(
                                "<red><lang:'text.graves.gui.break_grave'>",
                                "<dark_red><bold><lang:'text.graves.gui.cant_reverse'>",
                                "",
                                "<white><lang:'text.graves.gui.click_to_confirm'>"
                        )
                )
        );

        @SerializedName("quick_pickup_button")
        public IconData quickPickupButton = IconData.of(IconItem.Texture.QUICK_PICKUP, "<red><lang:'text.graves.gui.quick_pickup'>");

        @SerializedName("fetch_button")
        public Variant<IconData> fetchButton = Variant.of(
                IconData.of(Items.LEAD, "<yellow><lang:'text.graves.gui.fetch'>"),
                IconData.of(Items.LEAD, "<yellow><lang:'text.graves.gui.fetch'>")
        );

        @SerializedName("teleport_button")
        public Variant<IconData> teleportButton = Variant.of(
                IconData.of(Items.ENDER_PEARL, "<#a52dfa><lang:'text.graves.gui.teleport'>", "<white><lang:'text.graves.gui.teleport.cost'> <#cfcfcf>${item} × ${count}"),
                IconData.of(Items.ENDER_PEARL, "<dark_gray><lang:'text.graves.gui.teleport'>", "<white><lang:'text.graves.gui.teleport.cost'> <#cfcfcf>${item} × ${count} <gray>(<red><lang:'text.graves.gui.teleport.not_enough'></red>)")
        );

        @SerializedName("back_button")
        public IconData backButton = IconData.of(Items.STRUCTURE_VOID, "<red><lang:'text.graves.gui.quick_pickup'>");

        @SerializedName("bar")
        public IconData barButton = IconData.of(Items.WHITE_STAINED_GLASS_PANE, "");
    }

    @SerializedName("text")
    public Texts texts = new Texts();

    public static class Texts {
        @SerializedName("grave_created")
        public WrappedText messageGraveCreated = ofText("<white><lang:'text.graves.created_at_expire':'<yellow>${position}':'<gray>${world}':'<red>${break_time}'>");
        @SerializedName("protection_ended")
        public WrappedText messageProtectionEnded = ofText("<red><lang:'text.graves.no_longer_protected':'<gold>${position}':'<white>${world}':'<yellow>${item_count}'>");
        @SerializedName("grave_expired")
        public WrappedText messageGraveExpired = ofText( "<red><lang:'text.graves.expired':'<gold>${position}':'<white>${world}':'<yellow>${item_count}'>");
        @SerializedName("grave_broken")
        public WrappedText messageGraveBroken = ofText("<gray><lang:'text.graves.somebody_broke':'<white>${position}':'<white>${world}':'<white>${item_count}'>");

        @SerializedName("years_suffix")
        public String yearsText = "y";
        @SerializedName("days_suffix")
        public String daysText = "d";
        @SerializedName("hours_suffix")

        public String hoursText = "h";
        @SerializedName("minutes_suffix")

        public String minutesText = "m";
        @SerializedName("seconds_suffix")

        public String secondsText = "s";
        @SerializedName("infinity")
        public String infinityText = "∞";

        @SerializedName("date_format")
        public WrappedDateFormat fullDateFormat = WrappedDateFormat.of("dd.MM.yyyy, HH:mm");

        @SerializedName("world_names")
        public Map<Identifier, WrappedText> worldNameOverrides = new HashMap<>();
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

    public void fillMissing() {
        for (var id : CommonProtection.getProviderIds()) {
            if (!id.getNamespace().equals("universal_graves") && !this.placement.blockInProtection.containsKey(id.toString())) {
                this.placement.blockInProtection.put(id, false);
            }
        }
    }

    public String getFormattedTime(long time) {
        if (time < 0) {
            return "0" + this.texts.secondsText;
        }

        long seconds = time % 60;
        long minutes = (time / 60) % 60;
        long hours = (time / (60 * 60)) % 24;
        long days = time / (60 * 60 * 24) % 365;
        long years = time / (60 * 60 * 24 * 365);

        StringBuilder builder = new StringBuilder();

        if (years > 0) {
            builder.append(years).append(this.texts.yearsText);
        }
        if (days > 0) {
            builder.append(days).append(this.texts.daysText);
        }
        if (hours > 0) {
            builder.append(hours).append(this.texts.hoursText);
        }
        if (minutes > 0) {
            builder.append(minutes).append(this.texts.minutesText);
        }
        if (seconds > 0 || time <= 0) {
            builder.append(seconds).append(this.texts.secondsText);
        }
        return builder.toString();
    }


    public static WrappedText ofText(String s) {
        return WrappedText.of(s);
    }

    public class Arena {
        public int x1;
        public int y1;
        public int z1;
        public int x2;
        public int y2;
        public int z2;

        public boolean contains(int x, int y, int z) {
            return x1 < x && x2 > x && y1 < y && y2 > y && z1 < z && z2 > z;
        }
    }
}
