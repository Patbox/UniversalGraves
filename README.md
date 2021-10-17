![Logo](https://i.imgur.com/otnGmMi.png)
# Universal Graves
It's a simple, but really customisable grave/death chest mod! 
You can change how every message, block and hologram looks, how long it will be protected,
if should drop items after expiring and alike.

*This mod works only on Fabric Mod Loader and compatible!*

If you have any questions, you can ask them on my [Discord](https://pb4.eu/discord)

[Also check out my other mods and project, as you might find them useful!](https://pb4.eu)

![Example image](https://i.imgur.com/hfyd10Q.png)

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
  "xpStorageType": "vanilla",                // Allows to change how much of xp is stored, "none" for nothing/destroying, "vanilla" for vanilla amount, "percent_levels" for percent of levels, "percent_points" for percent of points and "drop" for dropping outside of grave
  "xpPercentTypeValue": 100.0,               // Changes how much percent of xp will be stored, works only with xpStorageType of `percent_...`
  "createFromPvP": true,                     // If false, after dying from another players attack grave won't be created
  "createInClaims": true,                    // if false, graves won't be created in claims
  "dropItemsAfterExpiring": true,            // If items should drop breaking from expiration
  "hologram": true,                          // Enables hologram
  "hologramOffset": 1.2,                     // Changes vertical offset of hologram
  "hologramProtectedText": [/*...*/],        // Hologram lines while protected
  "hologramText": [/*...*/],                 // Hologram lines while not protected
  "guiTitle": "...",                         // Gui title of list of player's graves
  "guiProtectedText": [/*...*/],             // Gui text while protected
  "guiText": [/*...*/],                      // Gui text while protected
  "message[Type]": "...",                    // Allows to change message, leave empty ("") to disable it
  "graveTitle": "..."                        // Changes grave title
}
```