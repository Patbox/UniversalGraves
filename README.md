<img src="https://i.imgur.com/bzeKsL1.png" width="256px"/>

# Universal Graves
It's a simple, but really customisable grave/death chest mod! 
You can change how every message, block and hologram looks, how long it will be protected,
if should drop items after expiring and alike.

This mod isn't require on client, but adding it allows for usage of more models.

*This mod works only on Fabric Mod Loader and compatible!*

If you have any questions, you can ask them on my [Discord](https://pb4.eu/discord)

[Also check out my other mods and project, as you might find them useful!](https://pb4.eu)

## Grave styles

* Default, `player_head`, recommended for servers
  - Style for vanilla clients: 
    ![Example image](https://i.imgur.com/hfyd10Q.png)
  - Style for clients with mod:
    ![Example image](https://i.imgur.com/045tdtV.png)
* `client_model` style with client mod and `hologramDisplayIfOnClient` set to false, recommended for modpacks
  ![Example image](https://i.imgur.com/lH0DwVK.png)
* `preset_head`:
  ![Example image](https://i.imgur.com/lH0DwVK.png)


## Commands (and permissions):
- `/graves` - Main command, shows list of users graves (`universal_graves.list`, available by default)
- `/graves player <player>` - Opens gui with list of players graves (`universal_graves.list_others`)
- `/graves reload` - Reloads configuration and styles (requires `universal_graves.reload`)

Additionally, by having `universal_graves.teleport` permission, you can teleport to any grave.

## Configuration:
You can find config file in `./config/unicversal-graves.json`.
[Formatting uses Simplified Text for which docs you can find here]().
Additionally, every message type has few own local variables.

```json5
{
  "CONFIG_VERSION_DONT_TOUCH_THIS": 2,
  "graveStyle": "player_head",               // Changes how block appears, "player_head" for owner's head, "preset_head" for head using values below, "chest" for chest, "barrel" for barrel and "custom" for custom
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
  "createFromPvP": true,                     // If false, after dying from another players attack grave won't be created
  "createFromVoid": true,                    // If false, graves won't be created if player dies in void
  "createFromCommandDeaths": true,           // If false, graves won't be created if player dies because of kill command
  "createInClaims": true,                    // if false, graves won't be created in claims
  "dropItemsAfterExpiring": true,            // If items should drop breaking from expiration
  "shiftClickTakesItems": true,              // Enables quick pickup of graves if clicked while sneaking
  "allowAttackersToTakeItems": false,        // Allows attackers to take items from victim's grave
  "graveTitle": "...",                        // Changes grave title
  "hologram": true,                          // Enables hologram
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
  "worldNameOverrides": {                    // Allows to override name of the world in messages
    "world:id": "WorldName"
    /* and others */
  },
  "blacklistedWorlds": [                     // Allows to block creation of graves with worlds with matching id
    /*...*/
  ],
  "tryDetectionSoulbound": true,             // Toggles automatic detection of soulbound enchantment (it's hit or miss)
  "skippedEnchantments": [                   // Allows to add own enchantments that end up being skipped from adding to grave
    /*...*/
  ]          
}
```