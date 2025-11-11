# SMPTools

## Description
SMPTools is a comprehensive Minecraft Paper plugin designed to enhance the survival multiplayer (SMP) experience with a variety of useful features and quality-of-life improvements. From player-specific commands like flight and private vaults to server-wide leaderboards and customizable chat options, SMPTools aims to provide a richer and more engaging gameplay environment.

## Features

### Core Commands & Systems
*   **/fly**: Toggles flight mode for players.
*   **/pv**: Opens a private vault for personal storage.
*   **/sethome <name>**: Sets a personal home location.
*   **/home <name>**: Teleports to a saved home location.
*   **/delhome <name>**: Deletes a saved home location.
*   **/homes**: Lists all saved home locations.
*   **/msg <player> <message>**: Sends a private message to another player.
*   **/stats [player]**: Displays your or another player's statistics.
*   **/clearstats <player>**: Clears a player's statistics (requires permission).
*   **/prefix**: Opens a GUI for selecting chat prefixes.
*   **/color**: Opens a GUI for selecting chat colors.
*   **/leaderboard**: Displays the server-wide statistics leaderboard.
*   **NameTag Customization**: Custom name tags for players.
*   **Sleep Mechanics**: Enhanced sleep functionality.
*   **Player Statistics**: Tracks various player statistics.
*   **Vault Integration**: Integration with a vault system.
*   **Join/Leave Messages**: Custom messages for players joining and leaving.
*   **Chat Formatting**: Customizable chat prefixes and colors.
*   **Tab Health Display**: Shows player health in the tab list.

### New Features & Enhancements

*   **Tags/Titles System**:
    *   **`/tags`**: Opens a GUI to view and select unlocked titles.
    *   **`/tags set <player> <title>`**: Admin command to unlock titles for players.
    *   **Milestone-based Unlocks**: Titles are unlocked by achieving various in-game milestones (e.g., blocks broken, playtime, boss kills, item acquisition).
    *   **Dynamic Display**: Equipped titles appear in chat, name tags, and join/leave messages (e.g., `[Title] PlayerName`).
    *   **Hover Descriptions**: Hovering over a player's title in chat displays its unlock description.
    *   **Configurable**: Milestones are defined in `tags.yml`.

*   **Player Teleportation (TPA System)**:
    *   **`/tpr <player>`**: Sends a teleport request to another player.
    *   **`/tpa`**: Accepts a pending teleport request.
    *   **`/tpd`**: Denies a pending teleport request.
    *   **`/tptoggle`**: Toggles acceptance of teleport requests.
    *   **Timeout System**: Requests expire after 60 seconds.

*   **Sit on Stairs**:
    *   Players can right-click on stairs with an empty hand to sit down.
    *   Toggleable via `features.sit-on-stairs.enabled` in `config.yml`.

*   **Image-to-Map**:
    *   **`/tomap <url>`**: Renders an image from a URL onto a Minecraft map.
    *   **Persistence**: Created maps now persist across server restarts, automatically reloading their images.
    *   **Configurable**: Toggleable via `features.image-to-map.enabled` in `config.yml`.

*   **In-Game Music Player**:
    *   **`/music play <url_or_name>`**: Plays an `.nbs` (Note Block Studio) song file from a URL for nearby players.
    *   **`/music broadcast <url_or_name>`**: Plays an `.nbs` song for all players on the server.
    *   **`/music stop`**: Stops the currently playing song.
    *   **GitHub Integration**: Supports playing songs directly from GitHub repositories by providing just the filename (e.g., `/music play My Song`).
    *   **Configurable**: Toggleable via `features.music-player.enabled` and `features.music-player.base-url` in `config.yml`.

*   **Funny Death Messages**:
    *   Replaces standard Minecraft death messages with humorous, meme-inspired alternatives based on the cause of death.
    *   Toggleable via `features.funny-death-messages.enabled` in `config.yml`.

*   **Ride Command**:
    *   **`/ride`**: Allows players to ride other entities (including other players) they are looking at. Use `/ride` again to dismount.
    *   Toggleable via `features.ride.enabled` in `config.yml`.

*   **Meme Sound Player**:
    *   **`/sound <sound_name>`**: Plays a custom meme sound effect for nearby players.
    *   **Configurable**: Toggleable via `features.meme-sounds.enabled` in `config.yml`.
    *   **Requires Resource Pack**: This feature requires a custom resource pack to be created and hosted (e.g., on GitHub) for the sounds to be heard by players.

## Installation
1.  Download the latest `SMPTools.jar` from the releases page (or build it yourself using Maven).
2.  Place the `SMPTools.jar` file into your Minecraft Paper server's `plugins/` folder.
3.  Restart your server.
4.  Upon first run, `config.yml`, `stats.yml`, `tags.yml`, and `imagemaps.yml` will be generated in the `plugins/SMPTools/` folder.

## Usage

### Permissions
*   `smptools.fly`: Allows use of the `/fly` command.
*   `smptools.privatevault`: Allows use of the `/pv` command.
*   `smptools.clearstats`: Allows use of the `/clearstats` command.
*   `smptools.tags.admin`: Allows use of `/tags set` command.
*   `smptools.imagemap`: Allows use of the `/tomap` command.
*   `smptools.music`: Allows use of the `/music` command.
*   `smptools.ride`: Allows use of the `/ride` command.
*   `smptools.sound`: Allows use of the `/sound` command.
*   *(Other commands may have default permissions or be accessible to all players)*

### Configuration
*   **`config.yml`**: This file contains general plugin configurations, including feature toggles, daily reward settings, custom enchantment details, music player base URL, and meme sound resource pack URL.
*   **`stats.yml`**: This file stores player statistics. It is managed by the plugin and generally should not be manually edited unless you know what you are doing.
*   **`tags.yml`**: This file defines the milestone tags and their unlock requirements.
*   **`imagemaps.yml`**: This file stores the URLs for custom image maps to ensure they persist across restarts.

## Building from Source
SMPTools uses Maven for dependency management and building. To build the plugin:

1.  Clone the repository:
    ```bash
    git clone https://github.com/Berna/SMPTools.git
    cd SMPTools
    ```
2.  Build the project using Maven:
    ```bash
    mvn clean package
    ```
3.  The compiled `.jar` file will be located in the `target/` directory.

## Author
Berna

## TODO
*   **Meme Sound Player Resource Pack**: To fully enable the Meme Sound Player (`/sound` command), you need to create a custom Minecraft resource pack containing your desired sound files. This resource pack must then be hosted online (e.g., on GitHub, similar to how `.nbs` files are hosted). Once hosted, update the `features.meme-sounds.resource-pack-url` setting in `config.yml` with the direct download link to your resource pack's `.zip` file.