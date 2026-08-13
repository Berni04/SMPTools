# SMPTools

## Description
SMPTools is a comprehensive Minecraft Paper plugin designed to enhance the survival multiplayer (SMP) experience with a variety of useful features and quality-of-life improvements. From player-specific commands like flight and private vaults to server-wide leaderboards, a full quest system, and seasonal events, SMPTools aims to provide a richer and more engaging gameplay environment.

## Features

### Core Commands & Systems
*   **/fly**: Toggles flight mode for players.
*   **/pv**: Opens a private vault for personal storage.
*   **/sethome <name>**: Sets a personal home location.
*   **/home <name>**: Teleports to a saved home location.
*   **/delhome <name>**: Deletes a saved home location.
*   **/homes**: Lists all saved home locations.
*   **/stats [player]**: Displays your or another player's statistics.
*   **/clearstats <player>**: Clears a player's statistics (requires permission).
*   **/prefix**: Opens a GUI for selecting chat prefixes.
*   **/color**: Opens a GUI for selecting chat colors.
*   **/leaderboard**: Displays the server-wide statistics leaderboard.
*   **NameTag Customization**: Custom name tags for players.
*   **Player Statistics**: Tracks various player statistics with optimized periodic saving.
*   **Tab Health Display**: Shows player health in the tab list.

### Player Interaction
*   **Private Messaging**:
    *   **/msg <player> <message>**: Sends a private message to another player.
    *   **/r <message>**: Replies to the last private message received.
*   **Player Teleportation (TPA System)**:
    *   **/tpr <player>**: Sends a teleport request to another player.
    *   **/tpa**: Accepts a pending teleport request.
    *   **/tpd**: Denies a pending teleport request.
    *   **/tptoggle**: Toggles acceptance of teleport requests.
    *   **Timed Teleportation**: 3-second countdown that cancels on movement or damage.
    *   **Timeout System**: Requests expire after 60 seconds.
*   **Item-Based Bounty System**:
    *   **/bounty place <player>**: Places a bounty on a target player using held items as the reward.
    *   **/bounty list**: Opens an interactive GUI displaying all active bounties.
    *   **/bounty top**: Displays the highest-reward bounties on the server.
    *   **Automated Claims**: Eliminating a wanted player automatically transfers reward items to the hunter.
*   **Secure Remote Trading**:
    *   **/trade <player>**: Sends a remote item trade request to another player.
    *   **/trade <accept|deny|cancel>**: Accepts, denies, or cancels pending trade requests.
    *   **Interactive GUI**: Dual-panel trade GUI with real-time item inspection, lock-in confirmation, and safety anti-scam countdowns.
*   **AFK Management**:
    *   **/afk**: Toggles your AFK (Away From Keyboard) status manually.
    *   **Automatic Detection**: Automatically marks players as AFK after a configurable inactivity threshold.
*   **Vote-Based Sleep**: Clickable Accept/Deny system for skipping the night.

### Customization
*   **Tags/Titles System**:
    *   **/tags**: Opens a GUI to view and select unlocked titles.
    *   **/tags set <player> <title>**: Admin command to unlock titles for players.
    *   **Milestone-based Unlocks**: Titles are unlocked by achieving various in-game milestones (e.g., blocks broken, playtime, boss kills, mob kills, crafting, movement).
    *   **Dynamic Display**: Equipped titles appear in chat, name tags, and join/leave messages (e.g., `[Title] PlayerName`).
    *   **Hover Descriptions**: Hovering over a player's title in chat displays its unlock description.
    *   **Configurable**: Milestones are defined in `tags.yml`.
*   **Cosmetic Particle Trails**:
    *   **/trails**: Opens an interactive GUI to choose ambient particle effects (Flame, Hearts, Ender, Rainbow, etc.) that follow your player movement.
    *   **Permission-gated**: Individual trail access controlled via permissions and persistent across sessions.
*   **Chat Formatting**: Customizable chat prefixes and colors with centralized ChatManager.
*   **Item Renaming**: **/rename <name>**: Renames the item in your hand with MiniMessage formatting support.
*   **Join/Leave Messages**: Custom messages for players joining and leaving.

### Administration & Container Protection
*   **Signless Container Locks**:
    *   **/lock**: Locks the chest or container block you are looking at without requiring physical signs.
    *   **/unlock**: Unlocks your owned container.
    *   **/trust <player>**: Grants trusted access to a specified player on your locked container.
    *   **/untrust <player>**: Revokes access for a trusted player.
    *   **Smart Double-Chest Protection**: Automatically pairs and protects double-chests and preserves Access Control Lists (ACLs).
*   **Player Graves**: Vanilla-style graves spawn at death location with holograms and looting.
*   **Chunk Loaders**: **/givechunkloader <player>**: Gives a chunk loader item that force-loads chunks even when no players are online.
*   **Inventory Viewer**: **/invsee <player>**: View another player's inventory with a death-screen-like layout showing armor and off-hand.
*   **Troll Commands**: **/troll <player>**: Opens a GUI with 20+ troll options including Chat Scramble, Fake Lag, Fake Join/Leave, and more.
*   **Force Execute**: **/sudo <player> <command>**: Forces a player to execute a command or send a chat message.
*   **Custom Items (WIP)**: **/customitem <type> <data>**: Gives custom items with specified CustomModelData (work in progress).

### NPC & Dialogue System
*   **NPC Management**: **/npc <spawn|remove|reload>**: Create and manage NPCs with custom skins and names.
*   **Dialogue System**: NPCs can have interactive dialogues with multiple options and player response bubbles.
*   **Mission NPCs**: NPCs that serve as quest givers for the Missions system.

### Missions System
*   **Missions GUI**: **/missions**: Opens the mission selection GUI with questlines and categories.
*   **Mission Types**: Various mission types including collection, killing, crafting, and exploration.
*   **Questlines**: Sequential missions with prerequisites and story progression.
*   **Rewards**: Configurable rewards for completing missions.
*   **Persistence**: Mission progress auto-saves every 5 minutes and persists across restarts.
*   **Reset**: **/missions resetquestline**: Resets your active questline to start over.
*   **Configurable**: Missions defined in `missions.yml`, dialogues in `dialogues.yml`.

### MMO Skills System
*   **/skills**: View your skill levels and experience.
*   **Mining Skill**: Earn experience from mining ores with double drop chance.
*   **Woodcutting Skill**: Earn experience from chopping trees with double drop chance.
*   **Excavation Skill**: Earn experience from digging with treasure hunter perk.
*   **Combat Skill**: Earn experience from killing mobs with critical strike chance.
*   **Configurable**: All skill formulas and chances can be customized in `config.yml`.

### Dynamic Mini-Events Engine
*   **/event** (alias **/events**): Opens the server events dashboard GUI displaying live event status, time remaining, and Top 5 leaderboards.
*   **/event start <type> [duration]**: Admin command to manually trigger an automated mini-event session.
*   **/event stop**: Admin command to conclude an active mini-event.
*   **6 Automated Event Types**:
    *   🎣 **Fishing Derby** (15m): Point values based on fish rarity (Cod, Salmon, Tropical, Pufferfish, Treasure) with Angler Streak combo multipliers.
    *   ⛏️ **Ore Rush** (10m): Ore point scoring with passive 2.0x Ore drops and 2.0x Mining Skill XP.
    *   ⚔️ **Mob Frenzy** (15m): Mob kill scoring with passive 2.0x Mob drops and Slayer killstreaks.
    *   ⚡ **Double XP Hour** (60m): Applies global 2.0x Skill XP across all MMO skills.
    *   🌾 **Harvest Sprint** (10m): Crop harvest scoring with Speed II passive boost.
    *   🗺️ **Treasure Dig** (10m): Excavation scoring with buried treasure pouch drops.
*   **Dual HUD Display**: Real-time event leaderboards render on both the sidebar **Scoreboard** and top **BossBar** simultaneously (individually toggleable in `events.yml`).

### Custom Utility Artifacts & Equipment Pouch
*   **/artifacts** (alias **/artifact**, **/pouch**): Opens the 27-slot Artifact Equipment Pouch GUI to equip passive artifacts without occupying hotbar or offhand slots.
*   **/artifacts give <player> <type>**: Admin command to grant custom artifacts to players.
*   **22 Custom Artifacts**:
    *   *Mobility*: Grappling Hook, Wind Dash Feather, Leap Frog Boots, Shadow Step Dagger, Feather Glider Ring.
    *   *Utility & QoL*: Portable Workbench, Homing Compass, Magnet Totem, Abyssal Lantern, Void Saver Charm, Alchemist's Satchel, Auto-Feeder Satchel, Jack's Pumpkin Helmet.
    *   *Combat*: Vampiric Scythe, Phoenix Feather, Sonic Wave Horn, Dragon Breath Cannon.
    *   *Gathering & Farming*: Nature's Touch Hoe, Timber Axe, Ore Radar Scanner, Chlorophyll Band, Master Angler's Lure.

### Seasonal Events System
*   **/seasonal** (alias **/seasons**, **/season**): Opens the central 27-slot Seasonal Events Hub GUI displaying the active season, calendar schedule, and quick-launch event shortcuts.
*   **/seasonal start <season>**: Admin command to force-activate a season (e.g. `halloween`, `easter`, `christmas`, `black_friday`, `summer`).
*   **/seasonal reset**: Admin command to return season detection to real-world calendar dates.
*   **/halloween** (alias **/pumpkins**): Opens the 20-slot Spooky Pumpkin Hunt checklist GUI. Right-clicking hidden pumpkins in the world grants mini-rewards and tracks discovery progress. Completing the hunt unlocks **Jack's Pumpkin Helmet Artifact** and 16 Diamonds!
*   **/halloween setpumpkin <id> [hint]**: Admin command to turn targeted block into a scavenger hunt pumpkin.
*   **/easter** (alias **/eggs**): Opens the 15-slot Easter Egg Hunt checklist GUI. Finding all 15 hidden eggs awards the **Chlorophyll Band Artifact** + 12 Diamonds + 32 Golden Carrots!
*   **/easter setegg <id> [hint]**: Admin command to set targeted block as an Easter egg.
*   **/summer**: Displays Summer Heatwave & Solar Flare status (grants midday Haste II & Speed I buffs under open skies).
*   **Trick-or-Treating**: Right-clicking villagers with a helmet during Halloween has a 70% chance for treats and a 30% chance for trick prank bats!

### Custom Enchantments
*   **/cenchant**: Apply custom enchantments to items.
*   **Telekinesis**: Automatically sends block drops to your inventory.
*   **Lumberjack**: Breaks an entire tree at once.
*   **Configurable**: Enchantment settings in `config.yml`.

### Daily Rewards
*   **/daily**: Claim your daily reward.
*   **Cooldown System**: Configurable cooldown period between claims.
*   **Reward Types**: Supports economy commands and item rewards.
*   **Configurable**: Rewards and cooldown in `config.yml`.

### Image-to-Map
*   **/tomap <url> [width] [height]**: Renders an image from a URL onto Minecraft maps.
*   **Multi-Map Support**: Create large posters by specifying width and height dimensions.
*   **Persistence**: Created maps persist across server restarts, automatically reloading their images.
*   **Configurable**: Toggleable via `features.image-to-map.enabled` in `config.yml`.

### In-Game Music Player
*   **/music play <url_or_name>**: Plays an `.nbs` (Note Block Studio) song file from a URL for nearby players.
*   **/music broadcast <url_or_name>**: Plays an `.nbs` song for all players on the server.
*   **/music stop**: Stops the currently playing song.
*   **GitHub Integration**: Supports playing songs directly from GitHub repositories by providing just the filename (e.g., `/music play My Song`).
*   **Configurable**: Toggleable via `features.music-player.enabled` and `features.music-player.base-url` in `config.yml`.

### Quality of Life
*   **Sit on Stairs**: Players can right-click on stairs with an empty hand to sit down.
*   **Ride Command**: **/ride**: Allows players to ride other entities (including other players) they are looking at. Use `/ride` again to dismount.
*   **Accelerated Growth**: Boosts crop and sapling growth speed across loaded chunks with configurable tick multipliers.
*   **Funny Death Messages**: Replaces standard Minecraft death messages with humorous, meme-inspired alternatives based on the cause of death.
*   **Meme Sound Player**: **/sound <sound_name>**: Plays a custom meme sound effect for nearby players. Requires a custom resource pack.

### Utility Commands
*   **/uptime**: Shows the server uptime.
*   **/ping [player]**: Shows your or another player's ping with color-coded latency display.

### Seasonal Events

#### Christmas Events
*   **Advent Calendar**: **/advent** (alias **/xmas**): Opens a daily reward GUI for December with 25 daily rewards (CET timezone).
*   **Secret Santa**: **/secretsanta** (alias **/ss**) **<join|target|deposit|claim>**: Full Secret Santa event lifecycle management.
*   **Present Hunt**: **/present <give|remove> [tier]**: Admin command to manage Christmas presents scattered around the world.
*   **Festive Mobs**: Mobs spawn with Santa/Elf hats, drop Candy Canes, and Creepers leave confetti.
*   **Snowball Warfare**: Snowballs deal damage, apply freezing effect, and have headshot mechanics.
*   **Christmas World**: Custom dimension with enforced night, snow, no mob spawning, Adventure mode, and disabled PvP/Elytra.
*   **Christmas Portal**: Snow Block portal that teleports players to the Christmas dimension.
*   **Krampus Boss Fight**: **/krampus <spawn>**: Spawns Krampus (Wither Skeleton) with Blindness and Pull abilities. Features kidnapping mechanic where lethal damage teleports players to a cage with Cage Guards.

#### Black Friday Event
*   **/blackfriday <start|stop|reload|status>**: Manages a Black Friday event with dynamic villager discounts (90% default).

## Installation
1.  Download the latest `SMPTools.jar` from the releases page (or build it yourself using Maven).
2.  Place the `SMPTools.jar` file into your Minecraft Paper server's `plugins/` folder.
3.  Restart your server.
4.  Upon first run, the following files will be generated in the `plugins/SMPTools/` folder:
    *   `config.yml` - General plugin configuration
    *   `stats.yml` - Player statistics
    *   `tags.yml` - Tag/title milestones
    *   `imagemaps.yml` - Image map URLs
    *   `messages.yml` - Customizable messages (NEW)
    *   `missions.yml` - Mission definitions
    *   `dialogues.yml` - NPC dialogue trees
    *   `npcs.yml` - NPC spawn locations
    *   `advent.yml` - Advent calendar rewards
    *   `christmas.yml` - Christmas event settings
    *   `presents.yml` - Present hunt configuration
    *   `blackfriday.yml` - Black Friday event settings
    *   `chunkloaders.yml` - Chunk loader locations
    *   `player_missions.yml` - Player mission progress

## Message Customization

All player-facing messages can be customized in `messages.yml`. The plugin uses [MiniMessage format](https://docs.advntr.dev/minimessage/format.html) for rich text styling.

### Dynamic Player Tags

The following tags are available in messages and will be automatically replaced with player-specific values:

| Tag | Description | Example Output |
|-----|-------------|----------------|
| `<player>` | Full formatted name (color + prefix + name + title) | `[Pro] PlayerName [Builder]` |
| `<player_name>` | Just the raw player name | `PlayerName` |
| `<player_name_color>` | Player name with their chosen color | `PlayerName` (in red) |
| `<player_color>` | Just the color tag | `<red>` |
| `<player_prefix>` | Just the prefix | `[Pro]` |
| `<player_title>` | Just the equipped title | `[Builder]` |

### Placeholder Variables

Messages can contain `{placeholder}` variables that are replaced at runtime:

```yaml
homes:
  set: "<player_name_color>, your home '<gold>{name}</gold>' has been set!"
```

Common placeholders:
- `{name}` - Home name, item name, etc.
- `{target}` - Target player name
- `{level}` - Skill level
- `{skill_name}` - Skill display name
- `{message}` - Chat message content

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/fly` | Toggles flight mode | `smptools.fly` |
| `/pv` | Opens private vault | `smptools.privatevault` |
| `/sethome <name>` | Sets a home location | Default |
| `/home <name>` | Teleports to home | Default |
| `/delhome <name>` | Deletes a home | Default |
| `/homes` | Lists all homes | Default |
| `/msg <player> <message>` | Private message | Default |
| `/r <message>` | Reply to last message | Default |
| `/stats [player]` | View statistics | Default |
| `/clearstats <player>` | Clear player stats | `smptools.clearstats` |
| `/prefix` | Prefix selection GUI | Default |
| `/color` | Color selection GUI | Default |
| `/leaderboard` | Server leaderboard | Default |
| `/tags` | Tags/titles GUI | Default |
| `/tags set <player> <title>` | Unlock title for player | `smptools.tags.set` |
| `/tpr <player>` | Send teleport request | Default |
| `/tpa` | Accept teleport request | Default |
| `/tpd` | Deny teleport request | Default |
| `/tptoggle` | Toggle teleport requests | Default |
| `/rename <name>` | Rename item (MiniMessage) | Default |
| `/daily` | Claim daily reward | Default |
| `/skills` | View skill levels | Default |
| `/cenchant` | Apply custom enchant | `smptools.customenchant` |
| `/tomap <url> [w] [h]` | Render image on map | `smptools.imagemap` |
| `/music <play\|broadcast\|stop>` | Music player | `smptools.music` |
| `/ride` | Ride entities | `smptools.ride` |
| `/sound <name>` | Play meme sound | `smptools.sound` |
| `/sleepvote` | Vote to skip night | Default |
| `/givechunkloader <player>` | Give chunk loader | `smptools.chunkloader.give` |
| `/invsee <player>` | View inventory | `smptools.invsee` |
| `/troll <player>` | Troll GUI | `smptools.troll` |
| `/missions` | Missions GUI | Default |
| `/missions resetquestline` | Reset questline | Default |
| `/sudo <player> <cmd>` | Force player action | `smptools.sudo` |
| `/customitem <type> <data>` | Give custom item (WIP) | `smptools.customitem` |
| `/advent` (alias `/xmas`) | Advent calendar | Default |
| `/secretsanta <cmd>` (alias `/ss`) | Secret Santa | Default |
| `/npc <spawn\|remove\|reload>` | NPC management | `smptools.npc.admin` |
| `/uptime` | Server uptime | Default |
| `/ping [player]` | Check ping | Default |
| `/present <give\|remove>` | Manage presents | `smptools.admin` |
| `/krampus <spawn>` | Spawn Krampus | `smptools.admin` |
| `/blackfriday <cmd>` | Black Friday event | `smptools.admin` |
| `/bounty <place\|list\|top>` | Item-based bounty system | Default |
| `/trails` | Cosmetic particle trails GUI | Default |
| `/lock` | Lock container block | Default |
| `/unlock` | Unlock container block | Default |
| `/trust <player>` | Trust player on locked container | Default |
| `/untrust <player>` | Revoke trust on container | Default |
| `/afk` | Toggle AFK status | Default |
| `/trade <player\|accept\|deny\|cancel>` | Remote item trade request | Default |
| `/event` (alias `/events`) | Server events dashboard & admin controls | Default |
| `/artifacts` (alias `/pouch`) | Artifact equipment pouch & admin give | Default |
| `/seasonal` (alias `/seasons`) | Central seasonal events hub & admin controls | Default |
| `/halloween` (alias `/pumpkins`) | Spooky Pumpkin Hunt checklist & admin placement | Default |
| `/easter` (alias `/eggs`) | Easter Egg Hunt checklist & admin placement | Default |
| `/summer` | Summer Heatwave & Solar Flare status | Default |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `smptools.fly` | Use `/fly` command | - |
| `smptools.privatevault` | Use `/pv` command | - |
| `smptools.homes.<group>` | Permission node for home limit groups (e.g., `smptools.homes.vip`) | - |
| `smptools.clearstats` | Use `/clearstats` command | - |
| `smptools.stats.rollback` | Rollback/reset player statistics | op |
| `smptools.tags.set` | Use `/tags set` command | op |
| `smptools.imagemap` | Use `/tomap` command | - |
| `smptools.music` | Use `/music` command | - |
| `smptools.music.broadcast` | Use `/music broadcast` subcommand | - |
| `smptools.ride` | Use `/ride` command | - |
| `smptools.sound` | Use `/sound` command | - |
| `smptools.advent.bypass` | Bypass date restrictions on Advent Calendar | op |
| `smptools.chunkloader.give` | Give chunk loaders | op |
| `smptools.invsee` | Use `/invsee` command | op |
| `smptools.troll` | Use `/troll` command | op |
| `smptools.sudo` | Use `/sudo` command | op |
| `smptools.npc.admin` | Manage NPCs | op |
| `smptools.missions.admin` | Spawn mission NPCs | op |
| `smptools.customenchant` | Apply custom enchants | - |
| `smptools.customitem` | Give custom items (WIP) | op |
| `smptools.trails.<trail_id>` | Access specific particle trail | default |
| `smptools.bounty.admin` | Admin bounty bypass and removal | op |
| `smptools.lock.bypass` | Admin container lock bypass | op |
| `smptools.events.admin` | Manage automated mini-events (`/event start/stop`) | op |
| `smptools.artifacts.admin` | Admin custom artifacts give (`/artifacts give`) | op |
| `smptools.seasonal.admin` | Manage seasonal events and set scavenger targets | op |
| `smptools.admin` | Admin commands (presents, krampus, black friday) | op |

## Configuration

### Main Configuration Files
*   **`config.yml`**: General plugin configuration including feature toggles, skill formulas, daily rewards, custom enchantments, storage provider backend (`flatfile`, `sqlite`, `mongodb`), music player settings, accelerated growth multiplier, and meme sound resource pack URL.
*   **`events.yml`**: Mini-events scheduler configuration, event durations, point values, and Scoreboard/BossBar HUD toggles.
*   **`seasonal.yml`**: Seasonal events calendar ranges, mini-rewards, and feature toggles.
*   **`stats.yml`**: Player statistics storage. Managed by the plugin - avoid manual editing.
*   **`tags.yml`**: Defines milestone tags and their unlock requirements.
*   **`messages.yml`**: Customizable player-facing messages with MiniMessage support and dynamic player tags.

### Mission & NPC Configuration
*   **`missions.yml`**: Mission definitions including objectives, rewards, and questlines.
*   **`dialogues.yml`**: NPC dialogue trees with multiple options and responses.
*   **`npcs.yml`**: NPC spawn locations, types, skins, and associated dialogues.

### Seasonal Event Configuration
*   **`seasonal.yml`**: General seasonal calendar and scavenger reward parameters.
*   **`seasonal_locations.yml`**: Coordinates and hints for hidden Halloween pumpkins and Easter eggs.
*   **`player_seasonal.yml`**: Per-player discovery progress and grand reward claim records.
*   **`advent.yml`**: Advent calendar daily rewards for December.
*   **`christmas.yml`**: Christmas event settings including world rules and portal configuration.
*   **`presents.yml`**: Present hunt configuration and tier definitions.
*   **`blackfriday.yml`**: Black Friday event settings including discount percentages.

### Storage & Data Files
*   **`artifacts.yml`**: Stores player equipped passive artifact items in their `/artifacts` pouch.
*   **`bounties.yml` / DB**: Stores active player bounties, reward items, and target data.
*   **`locks.yml` / DB**: Stores container protection locks, owner UUIDs, and trusted player ACLs.
*   **`trails.yml` / DB**: Stores player-selected cosmetic particle trail settings.
*   **`imagemaps.yml`**: Stores URLs for custom image maps to persist across restarts.
*   **`chunkloaders.yml`**: Stores chunk loader locations for persistence.
*   **`player_missions.yml`**: Stores player mission progress and completion status.

## Building from Source
SMPTools uses Maven for dependency management and building. To build the plugin:

1.  Clone the repository:
    ```bash
    git clone https://github.com/Berni04/SMPTools.git
    cd SMPTools
    ```
2.  Build the project using Maven:
    ```bash
    mvn clean package
    ```
3.  The compiled `.jar` file will be located in the `target/` directory.

## Requirements
*   Minecraft Paper server (1.21+)
*   Java 17 or higher

## Contributing

This project uses:
*   **Adventure API** for text components and MiniMessage formatting
*   **Maven** for build management
*   **JUnit 5** for unit testing

### Building
```bash
mvn clean package
```

### Running Tests
```bash
mvn test
```

## Author
berni
