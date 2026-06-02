package com.smp.smptools.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Centralized GUI title constants for all plugin inventories.
 * Uses MiniMessage-compatible Component titles.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public final class GUITitles {

    private static final TextColor TEAL = TextColor.fromHexString("#008B8B");
    private static final TextColor PURPLE = TextColor.fromHexString("#5B2C6F");
    private static final TextColor DARK_RED = TextColor.fromHexString("#8B0000");

    // Homes
    public static final Component HOMES_GUI = Component.text("Your Homes");

    // Stats
    public static Component statsGui(String playerName) {
        return Component.text(playerName + "'s Stats", TEAL);
    }

    public static Component deathsGui(String playerName) {
        return Component.text(playerName + "'s Deaths", TEAL);
    }

    public static Component deathDetailGui(String playerName, int index) {
        return Component.text(playerName + "'s Death #" + index, DARK_RED);
    }

    public static Component deathInventoryGui(String playerName, int index) {
        return Component.text(playerName + "'s Death #" + index + " Inventory", DARK_RED);
    }

    // Leaderboard
    public static final Component LEADERBOARD_GUI = Component.text("Leaderboards");

    // Skills
    public static final Component SKILLS_GUI = Component.text("Your Skills");

    // Tags
    public static final Component TAGS_GUI = Component.text("Your Titles");

    // Prefix
    public static final Component PREFIX_GUI = Component.text("Choose a Prefix", NamedTextColor.AQUA);

    // Missions
    public static final Component MISSIONS_GUI = Component.text("Mission Control");
    public static final Component MISSIONS_AVAILABLE_GUI = Component.text("Available Missions", NamedTextColor.GREEN);
    public static final Component MISSIONS_IN_PROGRESS_GUI = Component.text("In Progress", NamedTextColor.YELLOW);
    public static final Component MISSIONS_COMPLETED_GUI = Component.text("Completed Missions", NamedTextColor.AQUA);

    // Private Vault
    public static Component vaultGui() {
        return Component.text("Private Vault", PURPLE);
    }

    // Troll
    public static Component trollGui(String targetName) {
        return Component.text("Troll Menu - " + targetName, NamedTextColor.DARK_RED);
    }

    // Invsee
    public static Component invseeGui(String playerName) {
        return Component.text(playerName + "'s Inventory", TEAL);
    }

    // Secret Santa
    public static Component secretSantaDepositGui(String targetName) {
        return Component.text("Deposit Gift for " + targetName, NamedTextColor.RED);
    }

    // Advent
    public static final Component ADVENT_GUI = Component.text("Advent Calendar", NamedTextColor.GREEN);

    private GUITitles() {
        // Prevent instantiation
    }
}
