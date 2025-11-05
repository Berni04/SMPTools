package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderboardCommand implements CommandExecutor {

    private final SMPTools plugin;

    public LeaderboardCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        String stat = (args.length > 0) ? args[0] : "deaths";
        openLeaderboardGUI(player, stat);
        return true;
    }

    private void createDisplayItem(Inventory inv, Material material, int slot, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> loreList = new ArrayList<>();
        for (String s : lore) {
            loreList.add(s);
        }
        meta.setLore(loreList);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private Material getMaterialForMob(String mobName) {
        switch (mobName.toLowerCase()) {
            case "cow": return Material.BEEF;
            case "sheep": return Material.WHITE_WOOL;
            case "pig": return Material.PORKCHOP;
            case "chicken": return Material.FEATHER;
            case "turtle": return Material.TURTLE_HELMET;
            case "llama": return Material.LEATHER;
            case "rabbit": return Material.RABBIT_FOOT;
            case "zombie": return Material.ROTTEN_FLESH;
            case "skeleton": return Material.BONE;
            case "creeper": return Material.GUNPOWDER;
            case "enderman": return Material.ENDER_PEARL;
            case "witch": return Material.GLASS_BOTTLE;
            case "blaze": return Material.BLAZE_ROD;
            case "spider": return Material.SPIDER_EYE;
            case "cave_spider": return Material.SPIDER_EYE;
            case "phantom": return Material.PHANTOM_MEMBRANE;
            case "slime": return Material.SLIME_BALL;
            case "wither_skeleton": return Material.WITHER_SKELETON_SKULL;
            case "warden": return Material.ECHO_SHARD;
            default: return Material.STONE;
        }
    }

    private Material getMaterialForOre(String oreName) {
        switch (oreName.toLowerCase()) {
            case "diamond": return Material.DIAMOND;
            case "gold": return Material.GOLD_INGOT;
            case "iron": return Material.IRON_INGOT;
            case "redstone": return Material.REDSTONE;
            case "lapis": return Material.LAPIS_LAZULI;
            case "coal": return Material.COAL;
            case "emerald": return Material.EMERALD;
            default: return Material.STONE;
        }
    }

    public void openLeaderboardGUI(Player player, String stat) {
        Inventory leaderboardGUI = Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + "Leaderboard");

        // Add filter items
        createDisplayItem(leaderboardGUI, Material.PLAYER_HEAD, 0, ChatColor.GOLD + "Deaths", "deaths");
        createDisplayItem(leaderboardGUI, Material.DIAMOND_SWORD, 1, ChatColor.GOLD + "Player Kills", "player_kills");

        List<String> peacefulMobs = Arrays.asList("cow", "sheep", "pig", "chicken", "turtle", "llama", "rabbit");
        int peacefulMobSlot = 9;
        for (String mobName : peacefulMobs) {
            createDisplayItem(leaderboardGUI, getMaterialForMob(mobName), peacefulMobSlot++, ChatColor.GREEN + mobName.substring(0, 1).toUpperCase() + mobName.substring(1), "mob_kills." + mobName);
        }

        List<String> hostileMobs = Arrays.asList("zombie", "skeleton", "creeper", "enderman", "witch", "blaze", "spider", "cave_spider", "phantom", "slime", "wither_skeleton", "warden");
        int hostileMobSlot = 18;
        for (String mobName : hostileMobs) {
            createDisplayItem(leaderboardGUI, getMaterialForMob(mobName), hostileMobSlot++, ChatColor.RED + mobName.substring(0, 1).toUpperCase() + mobName.substring(1), "mob_kills." + mobName);
        }

        List<String> trackedOres = Arrays.asList("diamond", "gold", "iron", "redstone", "lapis", "coal", "emerald");
        int oreSlot = 36;
        for (String oreName : trackedOres) {
            createDisplayItem(leaderboardGUI, getMaterialForOre(oreName), oreSlot++, ChatColor.AQUA + oreName.substring(0, 1).toUpperCase() + oreName.substring(1), "ores_mined." + oreName);
        }

        // Get all player stats
        ConfigurationSection statsSection = plugin.getStatsConfig().getConfigurationSection("stats");
        if (statsSection == null) {
            player.sendMessage(ChatColor.RED + "No stats to display.");
            return;
        }

        List<Map.Entry<String, Integer>> leaderboard = new ArrayList<>();
        for (String uuidString : statsSection.getKeys(false)) {
            int value = statsSection.getInt(uuidString + "." + stat, 0);
            leaderboard.add(new java.util.AbstractMap.SimpleEntry<>(uuidString, value));
        }

        // Sort the leaderboard
        leaderboard.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Display top 10
        int slot = 45;
        for (int i = 0; i < Math.min(10, leaderboard.size()); i++) {
            Map.Entry<String, Integer> entry = leaderboard.get(i);
            UUID uuid = UUID.fromString(entry.getKey());
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            createDisplayItem(leaderboardGUI, Material.PAPER, slot++, ChatColor.GREEN + "#" + (i + 1) + " " + playerName, ChatColor.WHITE + stat + ": " + entry.getValue());
        }

        player.openInventory(leaderboardGUI);
    }
}