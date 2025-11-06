# SMPTools

## Description
SMPTools is a comprehensive Minecraft Paper plugin designed to enhance the survival multiplayer (SMP) experience with a variety of useful features and quality-of-life improvements. From player-specific commands like flight and private vaults to server-wide leaderboards and customizable chat options, SMPTools aims to provide a richer and more engaging gameplay environment.

## Features

### Commands
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

### Listeners & Other Features
*   **NameTag Customization**: Custom name tags for players.
*   **Sleep Mechanics**: Enhanced sleep functionality.
*   **Player Statistics**: Tracks various player statistics.
*   **Vault Integration**: Integration with a vault system.
*   **Join/Leave Messages**: Custom messages for players joining and leaving.
*   **Chat Formatting**: Customizable chat prefixes and colors.
*   **Tab Health Display**: Shows player health in the tab list.

## Installation
1.  Download the latest `SMPTools.jar` from the releases page (or build it yourself using Maven).
2.  Place the `SMPTools.jar` file into your Minecraft Paper server's `plugins/` folder.
3.  Restart your server.
4.  Upon first run, `config.yml` and `stats.yml` will be generated in the `plugins/SMPTools/` folder.

## Usage

### Permissions
*   `smptools.fly`: Allows use of the `/fly` command.
*   `smptools.privatevault`: Allows use of the `/pv` command.
*   `smptools.clearstats`: Allows use of the `/clearstats` command.
*   *(Other commands may have default permissions or be accessible to all players)*

### Configuration
*   **`config.yml`**: This file contains general plugin configurations. You can modify messages, enable/disable features, and adjust various settings here.
*   **`stats.yml`**: This file stores player statistics. It is managed by the plugin and generally should not be manually edited unless you know what you are doing.

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
