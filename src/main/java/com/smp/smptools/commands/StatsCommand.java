package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatsCommand implements CommandExecutor {

    private final SMPTools plugin;

    public StatsCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You must specify a player to view their stats.");
                return true;
            }
            showStatsGUI((Player) sender, (Player) sender);
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            showStatsGUI((Player) sender, target);
        }
        return true;
    }

    private void showStatsGUI(Player viewer, OfflinePlayer target) {
        String playerUUID = target.getUniqueId().toString();
        ConfigurationSection statsSection = plugin.getStatsConfig().getConfigurationSection("stats." + playerUUID);

        Inventory statsGUI = Bukkit.createInventory(null, 45, ChatColor.DARK_AQUA + target.getName() + "'s Stats");

        // General Stats
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        headMeta.setOwningPlayer(target);
        playerHead.setItemMeta(headMeta);

        createDisplayItem(statsGUI, playerHead, 4, ChatColor.GOLD + "General Stats",
                ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + (statsSection != null ? statsSection.getInt("deaths", 0) : 0),
                ChatColor.YELLOW + "Player Kills: " + ChatColor.WHITE + (statsSection != null ? statsSection.getInt("player_kills", 0) : 0));

        // Peaceful Mobs
        List<String> peacefulMobs = Arrays.asList("cow", "sheep", "pig", "chicken", "turtle", "llama", "rabbit");
        int peacefulMobSlot = 10;
        for (String mobName : peacefulMobs) {
            int mobKills = (statsSection != null && statsSection.contains("mob_kills." + mobName)) ? statsSection.getInt("mob_kills." + mobName) : 0;
            Material mobMaterial = getMaterialForMob(mobName);
            createDisplayItem(statsGUI, mobMaterial, peacefulMobSlot++, ChatColor.GREEN + mobName.substring(0, 1).toUpperCase() + mobName.substring(1),
                    ChatColor.WHITE + "Kills: " + mobKills);
        }

        // Hostile Mobs
        List<String> hostileMobs = Arrays.asList("zombie", "skeleton", "creeper", "enderman", "witch", "blaze", "spider");
        int hostileMobSlot = 19;
        for (String mobName : hostileMobs) {
            int mobKills = (statsSection != null && statsSection.contains("mob_kills." + mobName)) ? statsSection.getInt("mob_kills." + mobName) : 0;
            Material mobMaterial = getMaterialForMob(mobName);
            createDisplayItem(statsGUI, mobMaterial, hostileMobSlot++, ChatColor.RED + mobName.substring(0, 1).toUpperCase() + mobName.substring(1),
                    ChatColor.WHITE + "Kills: " + mobKills);
        }

        // Ores Mined
        List<String> trackedOres = Arrays.asList("diamond", "gold", "iron", "redstone", "lapis", "coal", "emerald", "netherite");
        int oreSlot = 28;
        for (String oreName : trackedOres) {
            int oreMined = (statsSection != null && statsSection.contains("ores_mined." + oreName)) ? statsSection.getInt("ores_mined." + oreName) : 0;
            Material oreMaterial = getMaterialForOre(oreName);
            createDisplayItem(statsGUI, oreMaterial, oreSlot++, ChatColor.AQUA + oreName.substring(0, 1).toUpperCase() + oreName.substring(1),
                    ChatColor.WHITE + "Mined: " + oreMined);
        }

        viewer.openInventory(statsGUI);
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
    
    private void createDisplayItem(Inventory inv, ItemStack item, int slot, String name, String... lore) {
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

    public void showDeathInfoGUI(Player viewer, OfflinePlayer target) {
        String playerUUID = target.getUniqueId().toString();
        List<String> deathInfo = plugin.getStatsConfig().getStringList("stats." + playerUUID + ".death_info");

        Inventory deathInfoGUI = Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + target.getName() + "'s Deaths");

        for (int i = 0; i < deathInfo.size(); i++) {
            createDisplayItem(deathInfoGUI, Material.PAPER, i, ChatColor.RED + "Death #" + (i + 1), deathInfo.get(i).split(", "));
        }

        viewer.openInventory(deathInfoGUI);
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
            case "netherite": return Material.NETHERITE_INGOT;
            default: return Material.STONE;
        }
    }
}
