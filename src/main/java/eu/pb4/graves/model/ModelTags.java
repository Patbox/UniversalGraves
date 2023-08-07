package eu.pb4.graves.model;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

public interface ModelTags {
    Identifier PLAYER_HEAD = new Identifier("graves", "player_head");
    Identifier IF_PROTECTED = new Identifier("graves", "if_protected");
    Identifier IF_UNPROTECTED = new Identifier("graves", "if_unprotected");
    Identifier IF_PLAYER_MADE = new Identifier("graves", "if_player_made");
    Identifier IF_NOT_PLAYER_MADE = new Identifier("graves", "if_not_player_made");
    Identifier IF_REQUIRE_PAYMENT = new Identifier("graves", "payment_required");
    Identifier IF_NOT_REQUIRE_PAYMENT = new Identifier("graves", "payment_not_required");
    Identifier IF_VISUAL = new Identifier("graves", "if_visual");
    Identifier IF_NOT_VISUAL = new Identifier("graves", "if_not_visual");
    Identifier ITEM = new Identifier("graves", "item");
    Identifier HAS_PROTECTION_TIMER = new Identifier("graves", "has_protection_timer");
    Identifier HAS_BREAKING_TIMER = new Identifier("graves", "has_breaking_timer");
    Identifier ROUND_YAW_TO_90 = new Identifier("graves", "round_yaw_to_multiply_of_90");
    Identifier EQUIPMENT_HELMET = new Identifier("graves", "equipment/helmet");
    Identifier EQUIPMENT_CHESTPLATE = new Identifier("graves", "equipment/chestplate");
    Identifier EQUIPMENT_LEGGINGS = new Identifier("graves", "equipment/leggings");
    Identifier EQUIPMENT_BOOTS = new Identifier("graves", "equipment/boots");
    Identifier EQUIPMENT_MAIN_HAND = new Identifier("graves", "equipment/main_hand");
    Identifier EQUIPMENT_OFFHAND_HAND = new Identifier("graves", "equipment/offhand_hand");

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
