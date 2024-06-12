package eu.pb4.graves.model;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

public interface ModelTags {
    Identifier PLAYER_HEAD = Identifier.of("graves", "player_head");
    Identifier IF_PROTECTED = Identifier.of("graves", "if_protected");
    Identifier IF_UNPROTECTED = Identifier.of("graves", "if_unprotected");
    Identifier IF_PLAYER_MADE = Identifier.of("graves", "if_player_made");
    Identifier IF_NOT_PLAYER_MADE = Identifier.of("graves", "if_not_player_made");
    Identifier IF_REQUIRE_PAYMENT = Identifier.of("graves", "payment_required");
    Identifier IF_NOT_REQUIRE_PAYMENT = Identifier.of("graves", "payment_not_required");
    Identifier IF_VISUAL = Identifier.of("graves", "if_visual");
    Identifier IF_NOT_VISUAL = Identifier.of("graves", "if_not_visual");
    Identifier ITEM = Identifier.of("graves", "item");
    Identifier HAS_PROTECTION_TIMER = Identifier.of("graves", "has_protection_timer");
    Identifier HAS_BREAKING_TIMER = Identifier.of("graves", "has_breaking_timer");
    Identifier ROUND_YAW_TO_90 = Identifier.of("graves", "round_yaw_to_multiply_of_90");
    Identifier EQUIPMENT_HELMET = Identifier.of("graves", "equipment/helmet");
    Identifier EQUIPMENT_CHESTPLATE = Identifier.of("graves", "equipment/chestplate");
    Identifier EQUIPMENT_LEGGINGS = Identifier.of("graves", "equipment/leggings");
    Identifier EQUIPMENT_BOOTS = Identifier.of("graves", "equipment/boots");
    Identifier EQUIPMENT_MAIN_HAND = Identifier.of("graves", "equipment/main_hand");
    Identifier EQUIPMENT_OFFHAND_HAND = Identifier.of("graves", "equipment/offhand_hand");

    Identifier[] EQUIPMENT = new Identifier[] { EQUIPMENT_MAIN_HAND, EQUIPMENT_OFFHAND_HAND, EQUIPMENT_HELMET, EQUIPMENT_CHESTPLATE, EQUIPMENT_LEGGINGS, EQUIPMENT_BOOTS };
    Pair<Identifier, EquipmentSlot>[] EQUIPMENT_WITH_SLOT = new Pair[] {
            new Pair<>(EQUIPMENT_MAIN_HAND, EquipmentSlot.MAINHAND),
            new Pair<>(EQUIPMENT_OFFHAND_HAND, EquipmentSlot.OFFHAND),
            new Pair<>(EQUIPMENT_HELMET, EquipmentSlot.HEAD),
            new Pair<>(EQUIPMENT_CHESTPLATE, EquipmentSlot.CHEST),
            new Pair<>(EQUIPMENT_LEGGINGS, EquipmentSlot.LEGS),
            new Pair<>(EQUIPMENT_BOOTS, EquipmentSlot.FEET)
    };
}
