<img src="https://i.imgur.com/PMONXxW.png" width="256px"/>

# Universal Graves
It's a simple, but really customisable grave/death chest mod! 
You can customise as you like, by modifying message, blocks and hologram texts, how long it will be protected,
if should drop items after expiring and alike.

This mod can work purely on servers, through for model support mod is required on clients!

*This mod works only on Fabric and Quilt!*

If you have any questions, you can ask them on my [Discord](https://pb4.eu/discord)

[Also check out my other mods and project, as you might find them useful!](https://pb4.eu)

### Using 2.x/1.19.x or older?
[Click here to view correct readme!](https://github.com/Patbox/UniversalGraves/blob/1.19.4/README.md)

## Images
![Default Grave look](https://i.imgur.com/rJoRmFT.png)

## Commands (and permissions):
- `/graves` - Main command, shows list of player's graves (`universal_graves.list`, available by default)
- `/graves player <player>` - Opens gui with list of selected player's graves (`universal_graves.list_others`)
- `/graves modify <player>` - Opens gui with list of executor's graves with ability to modify them (`universal_graves.modify`)
- `/graves reload` - Reloads configuration and styles (requires `universal_graves.reload`)

Additionally, by having `universal_graves.teleport` permission, you can teleport to any grave.

## Configuration:
You can find main config file in `./config/universal-graves/config.json`.
[Formatting uses Simplified Text for which docs you can find here](https://placeholders.pb4.eu/user/text-format/).
Additionally, every message type has few own local variables, which you type as `${variable}`.
Most grave specific messages support these variables: `player`, `protection_time`, `break_time`, `xp`,
`item_count`, `position`, `world`, `death_cause`, `minecraft_day`, `creation_date`, `since_creation`,
`id`.

There are few other data types:
* `{/* PREDICATE */}` - Uses predicate format, [see here for all supported ones.](https://github.com/Patbox/PredicateAPI/blob/1.20/BUILTIN.md)
* `{/* COST */}` - Format for specifying cost.
  ```json5
  {
    // Type of cost. Supports, "free", "creative", "item" and "level"
    "type": "",
    // Only used for "item", describes Item Stack
    "input": {/* ITEMSTACK */},
    // Amount of cost.
    "count": 0
  }
  ```
* `{/* ITEMSTACK */}` - represents an item.
  Simple format: `"minecraft:skeleton_skull"`
  Full format
  ```json5
  {
    "id": "minecraft:skeleton_skull",
    "Count": 1,
    // Optional nbt,
    "tag": {}
  }
  ```

```json5
{
  // Config version. Never modify it unless you want to risk data corruption!
  "config_version": 3,
  "protection": {
    // Time grave is protected against other players. Set to -1 to make it infinite. In seconds
    "non_owner_protection_time": 900,
    // Time after which grave destroys itself. Set to -1 to disable it. In seconds
    "self_destruction_time": 1800,
    // Makes grave drop items after it expires/self destructs
    "drop_items_on_expiration": true,
    // Allows attackers to access graves, useful in case of pvp oriented servers.
    "attackers_bypass_protection": false,
    // Makes grave use real time instead of only progressing when world/server is running.
    "use_real_time": false
  },
  "interactions": {
    // Cost for accessing grave after death, 
    "unlocking_cost": { /* COST */ },
    // Enables giving death compass, which points to player's grave.
    "give_death_compass": true,
    // Makes clicking grave open an ui.
    "enable_click_to_open_gui": true,
    // Enables quick pickup by shifting and clicking grave with empty hand.
    "shift_and_use_quick_pickup": true,
    // Allows players to remotely remove protection form grave.
    "allow_remote_protection_removal": true,
    // Allows players to remotely break grave.
    "allow_remote_breaking": true,
    // Allows players to remotely pay cost of unlocking the grave.
    "allow_remote_unlocking": false
  },
  "storage": {
    // Selects how much of experience is stored. 
    // Available types: "none", "vanilla", "drop", "percent_points", "percent_levels"
    "experience_type": "percent_points",
    // Amount of percent stored for "percent_X" types
    "experience_percent:setting_value": 100.0,
    // Allows to create graves with only XP
    "can_store_only_xp": false,
    // Enables usage of alternative experience orb entity, to prevent XP duplication with some mods.
    "alternative_experience_entity": false,
    // Blocks items with enchantments from being added to grave.
    "blocked_enchantments": [
      "somemod:soulbound"
    ]
  },
  "placement": {
    // Limits grave count per player. -1 is unlimited
    "player_grave_limit": -1,
    // Allows grave to replace any block
    "replace_any_block": false,
    // Max distance around starting position used for search of free spot.
    "max_distance_from_source_location": 8,
    // Allows shifting starting position on failure
    "shift_location_on_failure": true,
    // Max amount of shifts allowed.
    "max_shift_tries": 5,
    // Distance used for shifting.
    "max_shift_distance": 40,
    // Makes grave generate on top of fluids instead of within them.
    "generate_on_top_of_fluids": false,
    // Makes broken graves restore replaced block.
    "restore_replaced_block": false,
    // Makes broken graves restore replaced block after player breaks them.
    "restore_replaced_block_after_player_breaking": true,
    // Damage types that don't generate graves.
    "cancel_creation_for_damage_types": {
      "minecraft:fire": "... custom message or empty"
    },
    // Attacker's entity types preventing grave creation.
    "cancel_creation_for_ignored_attacker_types": {
      "minecraft:player": "... custom message or empty"
    },
    // Predicates blocking creation of graves
    "blocking_predicates": [{/* PREDICATE */}],
    // Blocks creation of graves in modded claim areas.
    "block_in_protected_area": {
      "goml:claim_protection": true
    },
    // Blocks creation of graves in entire worlds.
    "blacklisted_worlds": [
      "dungeons:world"
    ],
    // Blocks creation of graves in areas.
    "blacklisted_areas": {
      "minecraft:overworld": [
        {
          "x1": -100,
          "y1": -200,
          "z1": -100,
          "x2": 100,
          "y2": 500,
          "z2": 100,
        }
      ]
    },
    // Default grave creation failure message.
    "creation_default_failure_text": "...",
    // Grave creation failure in claim message.
    "creation_claim_failure_text": "..."
  },
  "teleportation": {
    // Teleportation cost
    "cost": {/* COST */},
    // Time before finalizing teleportation.
    "required_time": 5,
    // Y offset from grave location.
    "y_offset": 1.0,
    // Time for which player is invincible.
    "invincibility_time": 2,
    // Allows player to move while waiting.
    "allow_movement_while_waiting": false,
    // Messages send to player
    "text": {
      "timer": "...",
      "timer_allow_moving": "...",
      "location": "...",
      "canceled": "..."
    }
  },
  "model": {
    // Default model file name
    "default": "default",
    // Alternatives for getting grave model.
    "alternative": [
      {
        "require": {/* PREDICATE */},
        "model": "model_name"
      }
    ],
    // Enables workaround for geyser players, making graves look like skulls for them.
    "enable_geyser_workaround": true,
    // Base item fro visual/container grave.
    "gravestone_item_base": "minecraft:skeleton_skull",
    // NBT added to the item.
    "gravestone_item_nbt": {}
  },
  "ui": {/* UI DEFINITIONS, modify text, */},
  "text": {
    // Date format used in placeholders. Uses https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
    "date_format": "dd.MM.yyyy, HH:mm",
    // Overrides for world names.
    "world_names": {
      "custom:world": "Custom World!"
    }
  }
}
```

## Grave models
> Todo: Finish this section.

Grave models are stored in `config/universal-graves/models/`. By default, there should be `example.json` file, which is a copy of default style.
To override default grave style, copy that file and rename it to `default.json` or other name specified in config and define its style.
You can also use different name and alternatives, to make random/unique models.
