# Universal Graves
It's a simple, but really customisable grave/death chest mod! 
You can change how every message, block and hologram looks, how long it will be protected,
if should drop items after expiring and alike.

If you have any questions, you can ask them on my [Discord](https://discord.com/invite/AbqPPppgrd)

![Example image](https://i.imgur.com/hfyd10Q.png)

Fun fact, I died 237 times while making this mod + 35 on 2nd account (so far)

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
  "CONFIG_VERSION_DONT_TOUCH_THIS": 1,
  "graveType": "player_head",         // Changes how block appears, `player_head` for owner's head, `preset_head` for head using values below, `chest` for chest and `barrel` for barrel
  "lockedTexture": "...",             // Points to locked grave texture (`preset_head`), requires a value field (most sites have it described)
  "unlockedTexture": "...",           // Points to unlocked grave texture (`preset_head`), requires a value field
  "isProtected": true,                // Changes if graves should be protected by default
  "shouldProtectionExpire": true,     // If protection should have time limit
  "protectionTime": 300,              // Time for which graves should be protected (is seconds)
  "shouldBreak": true,                // Changes if grave should break after some time
  "breakAfter": 900,                  // Time after which grave will break
  "storeExperience": true,            // If true, experience points will be stored in grave
  "xpStorageType": "vanilla",         // Allows to change how much of xp is stored, `none` for nothing, `vanilla` for vanilla amount `percent_levels` for percent of levels and `percent_points` for percent of points
  "xpPercentTypeValue": 100.0,        // Changes how much percent of xp will be stored, works only with xpStorageType of `percent_...`
  "createGravesFromPvP": true,        // If false, after dying from another players attack grave won't be created
  "dropItemsAfterExpiring": true,     // If items should drop breaking from expiration
  "hologram": true,                   // Enables hologram
  "hologramProtectedText": [/*...*/], // Hologram lines while protected
  "hologramText": [/*...*/],          // Hologram lines while not protected
  "guiTitle": "...",                  // Gui title of list of player's graves
  "guiProtectedText": [/*...*/],      // Gui text while protected
  "guiText": [/*...*/],               // Gui text while protected
  "display[Type]Message": true,       // Enables displaying of message
  "[Type]Message": "...",             // Changes message formatting
  "graveTitle": "..."                 // Changes grave title
}
```