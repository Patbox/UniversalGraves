<img src="https://i.imgur.com/bzeKsL1.png" width="256px"/>

# Universal Graves
It's a simple, but really customisable grave/death chest mod! 
You can customise as you like, by modifying message, blocks and hologram texts, how long it will be protected,
if should drop items after expiring and alike.

This mod can work purely on servers, through for model support mod is required on clients!

*This mod works only on Fabric Mod Loader and compatible!*

If you have any questions, you can ask them on my [Discord](https://pb4.eu/discord)

[Also check out my other mods and project, as you might find them useful!](https://pb4.eu)

## Grave styles
To change grave style you need to modify config, see more info below
* Default, `player_head`, recommended for servers
  - Style for vanilla clients: 
    ![Example image](https://i.imgur.com/hfyd10Q.png)
  - Style for clients with mod:
    ![Example image](https://i.imgur.com/045tdtV.png)
* `client_model` style with client mod and `hologramDisplayIfOnClient` set to false, recommended for modpacks
  ![Example image](https://i.imgur.com/lH0DwVK.png)
* and few other ones, which you need to configure manually to use


## Commands (and permissions):
- `/graves` - Main command, shows list of users graves (`universal_graves.list`, available by default)
- `/graves player <player>` - Opens gui with list of players graves (`universal_graves.list_others`)
- `/graves reload` - Reloads configuration and styles (requires `universal_graves.reload`)

Additionally, by having `universal_graves.teleport` permission, you can teleport to any grave.

## Configuration:
You can find config file in `./config/universal-graves.json`.
[Formatting uses Simplified Text for which docs you can find here](https://placeholders.pb4.eu/user/text-format/).
Additionally, every message type has few own local variables, which you type as `${variable}`.

```json5
{
  "CONFIG_VERSION_DONT_TOUCH_THIS": 2,
  "graveStyle": "player_head",               // Changes how block appears, "player_head" for owner's head, "client_model" for client side model (for modpacks), "preset_head" for head using values below, "chest" for chest, "barrel" for barrel and "custom" for custom
  "allowClientSideStyle": true,              // Enables client side models
  "playerHeadTurnIntoSkulls": true,          // Makes unlocked graves look like skull while using "player_head" style
  "presetHeadLockedTexture": "...",          // Points to locked grave texture ("preset_head"), requires a value field (most sites have it described)
  "presetHeadUnlockedTexture": "...",        // Points to unlocked grave texture ("preset_head"), requires a value field
  "customBlockStateLockedStyles": ["..."],   // Custom visual blockstate for locked graves for style "custom". Limited to 16, uses same formatting as /setblock
  "customBlockStateUnlockedStyles": ["..."], // Custom visual blockstate for unlocked graves for style "custom". Limited to 16, uses same formatting as /setblock
  "isProtected": true,                       // Changes if graves should be protected by default
  "protectionTime": 300,                     // Time for which graves should be protected (is seconds), -1 for infinite
  "breakingTime": 900,                       // Time after which grave will break, -1 to disable breaking
  "breakEmptyGraves": true,                  // If true, empty graves will break automatically
  "xpStorageType": "vanilla",                // Allows to change how much of xp is stored, "none" for nothing/destroying, "vanilla" for vanilla amount, "percent_levels" for percent of levels, "percent_points" for percent of points and "drop" for dropping outside of grave
  "xpPercentTypeValue": 100.0,               // Changes how much percent of xp will be stored, works only with xpStorageType of `percent_...`
  "replaceAnyBlock": true,                   // Allows to replace solid blocks if there is no other spot to place a grave
  "maxPlacementDistance": 8,                 // Maximal distance grave can be created from player's death location. Too big values can cause lag
  "useRealTime": false,                      // Switches to using real time for measuring time instead of ingame one
  "createFromPvP": true,                     // If false, after dying from another players attack grave won't be created
  "createFromVoid": true,                    // If false, graves won't be created if player dies in void
  "createFromCommandDeaths": true,           // If false, graves won't be created if player dies because of kill command
  "createInClaims": true,                    // if false, graves won't be created in claims
  "dropItemsAfterExpiring": true,            // If items should drop breaking from expiration
  "allowAttackersToTakeItems": false,        // Allows attackers to take items from victim's grave
  "shiftClickTakesItems": true,              // Enables quick pickup of graves if clicked while sneaking
  "giveGraveCompass": true,                  // When enabled, player will get a compass pointing to their grave after dying
  "graveTitle": "...",                       // Changes grave title
  "hologram": true,                          // Enables hologram
  "hologramDisplayIfOnClient": true,         // Allows to toggle visibility of hologram for players having client side mod
  "hologramOffset": 1.2,                     // Changes vertical offset of hologram
  "hologramProtectedText": [/*...*/],        // Hologram lines while protected
  "hologramText": [/*...*/],                 // Hologram lines while not protected
  "guiTitle": "...",                         // Gui title of list of player's graves
  "guiProtectedText": [/*...*/],             // Gui text while protected
  "guiText": [/*...*/],                      // Gui text while protected
  "message[Type]": "...",                    // Allows to change message, leave empty ("") to disable it
  "guiProtectedItem": [/*...*/],             // Items used in guis to represent protected graves, they use same syntax as /give
  "guiItem": [/*...*/],                      // Items used in guis to represent graves, they use same syntax as /give
  "[time-type]Text": "...",                  // Text used to represent time symbol
  "fullDateFormat": "...",                   // Text used to represent full date, using standard java date format
  "worldNameOverrides": {                    // Allows to override name of the world in messages
    "world:id": "WorldName"
    /* and others */
  },
  "blacklistedWorlds": [                     // Allows to block creation of graves with worlds with matching id
    "world:id"
    /*...*/
  ],
  "blacklistedAreas": {                     // Allows to block area within a world
    "world:id2": [
      { 
        "x1": -10,
        "y1": -100,
        "z1": -20,
        "x2": 4,
        "y2": 256,
        "z2": 30,
      }
    ]
  },
  "blacklistedDamageSources": [              // Allows to blacklist damage sources by name from creating graves
    "damageSourceName"
    /*...*/                                  // Use /graves display_damage_sources to find correct names to entry
  ],
  "tryDetectionSoulbound": true,             // Toggles automatic detection of soulbound enchantment (it's hit or miss)
  "skippedEnchantments": [                   // Allows to add own enchantments that end up being skipped from adding to grave
    /*...*/
  ]          
}
```

Most grave specific messages support these variables: `player`, `protection_time`, `break_time`, `xp`, 
`item_count`, `position`, `world`, `death_cause`, `minecraft_day`, `creation_date`, `since_creation`, 
`id`.
