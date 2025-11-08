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
import java.util.Map;

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

        Inventory statsGUI = Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + target.getName() + "'s Stats");

        // Player Head and General Info
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        headMeta.setOwningPlayer(target);
        headMeta.setDisplayName(ChatColor.GOLD + target.getName());
        playerHead.setItemMeta(headMeta);
        statsGUI.setItem(4, playerHead);

        // General Stats
        long playtimeMinutes = statsSection != null ? statsSection.getLong("playtime_minutes", 0) : 0;
        createDisplayItem(statsGUI, Material.CLOCK, 20, ChatColor.GOLD + "Playtime", ChatColor.YELLOW + formatPlaytime(playtimeMinutes));
        createDisplayItem(statsGUI, Material.DIAMOND_SWORD, 21, ChatColor.GOLD + "Player Kills", statsSection != null ? statsSection.getInt("player_kills", 0) : 0);
        createDisplayItem(statsGUI, Material.SKELETON_SKULL, 22, ChatColor.GOLD + "Total Deaths", statsSection != null ? statsSection.getInt("deaths_total", 0) : 0);
        
        // Block Stats
        createDisplayItem(statsGUI, Material.DIAMOND_PICKAXE, 23, ChatColor.GOLD + "Blocks Broken", statsSection != null ? statsSection.getInt("blocks_broken", 0) : 0);
        createDisplayItem(statsGUI, Material.GRASS_BLOCK, 24, ChatColor.GOLD + "Blocks Placed", statsSection != null ? statsSection.getInt("blocks_placed", 0) : 0);

        // Ores Mined
        createDisplayItem(statsGUI, Material.COAL_ORE, 37, ChatColor.GOLD + "Coal Mined", statsSection != null ? statsSection.getInt("ores_mined.coal", 0) : 0);
        createDisplayItem(statsGUI, Material.IRON_ORE, 38, ChatColor.GOLD + "Iron Mined", statsSection != null ? statsSection.getInt("ores_mined.iron", 0) : 0);
        createDisplayItem(statsGUI, Material.GOLD_ORE, 39, ChatColor.GOLD + "Gold Mined", statsSection != null ? statsSection.getInt("ores_mined.gold", 0) : 0);
        createDisplayItem(statsGUI, Material.LAPIS_LAZULI, 40, ChatColor.GOLD + "Lapis Mined", statsSection != null ? statsSection.getInt("ores_mined.lapis", 0) : 0);
        createDisplayItem(statsGUI, Material.REDSTONE, 41, ChatColor.GOLD + "Redstone Mined", statsSection != null ? statsSection.getInt("ores_mined.redstone", 0) : 0);
        createDisplayItem(statsGUI, Material.DIAMOND, 42, ChatColor.GOLD + "Diamonds Mined", statsSection != null ? statsSection.getInt("ores_mined.diamond", 0) : 0);
        createDisplayItem(statsGUI, Material.EMERALD, 43, ChatColor.GOLD + "Emeralds Mined", statsSection != null ? statsSection.getInt("ores_mined.emerald", 0) : 0);

        // Deaths Button
        createDisplayItem(statsGUI, Material.PAPER, 49, ChatColor.RED + "View Deaths", ChatColor.YELLOW + "Click to see detailed death info");

        viewer.openInventory(statsGUI);
    }

    private String formatPlaytime(long totalMinutes) {
        if (totalMinutes < 0) {
            return "N/A";
        }

        long days = totalMinutes / 1440;
        long hours = (totalMinutes % 1440) / 60;
        long minutes = totalMinutes % 60;

        return String.format("%dD %dH %dM", days, hours, minutes);
    }

    private void createDisplayItem(Inventory inv, Material material, int slot, String name, int amount) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Amount: " + amount);
        meta.setLore(lore);
        item.setItemMeta(meta);
        item.setAmount(Math.max(1, Math.min(amount, 64))); // Amount between 1 and 64
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

        

        public void showDeathInfoGUI(Player viewer, OfflinePlayer target) {

            String playerUUID = target.getUniqueId().toString();

            List<Map<?, ?>> deathInfo = plugin.getStatsConfig().getMapList("stats." + playerUUID + ".deaths_info");

    

            Inventory deathInfoGUI = Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + target.getName() + "'s Deaths");

    

            for (int i = 0; i < deathInfo.size(); i++) {

                Map<?, ?> death = deathInfo.get(i);

                String time = (String) death.get("time");

                String cause = (String) death.get("cause");

                createDisplayItem(deathInfoGUI, Material.PAPER, i, ChatColor.RED + "Death #" + (i + 1),

                        ChatColor.YELLOW + "Time: " + ChatColor.WHITE + time,

                        ChatColor.YELLOW + "Cause: " + ChatColor.WHITE + cause,

                        ChatColor.GRAY + "Click for more info");

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

    public void showDetailedDeathInfoGUI(Player viewer, OfflinePlayer target, int deathIndex) {
        String playerUUID = target.getUniqueId().toString();
        List<Map<?, ?>> deathInfo = plugin.getStatsConfig().getMapList("stats." + playerUUID + ".deaths_info");
        Map<?, ?> death = deathInfo.get(deathIndex);

        Inventory detailedDeathInfoGUI = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + target.getName() + "'s Death #" + (deathIndex + 1));

        String time = (String) death.get("time");
        int x = (int) death.get("x");
        int y = (int) death.get("y");
        int z = (int) death.get("z");
        String cause = (String) death.get("cause");

        createDisplayItem(detailedDeathInfoGUI, Material.CLOCK, 10, ChatColor.YELLOW + "Time", ChatColor.WHITE + time);
        createDisplayItem(detailedDeathInfoGUI, Material.COMPASS, 12, ChatColor.YELLOW + "Coordinates", ChatColor.WHITE + "X: " + x + ", Y: " + y + ", Z: " + z);
        createDisplayItem(detailedDeathInfoGUI, Material.SKELETON_SKULL, 14, ChatColor.YELLOW + "Cause", ChatColor.WHITE + cause);
        createDisplayItem(detailedDeathInfoGUI, Material.CHEST, 16, ChatColor.YELLOW + "Inventory", ChatColor.GRAY + "Click to view");

        viewer.openInventory(detailedDeathInfoGUI);
    }

    public void showDeathInventoryGUI(Player viewer, OfflinePlayer target, int deathIndex) {
        String playerUUID = target.getUniqueId().toString();
        List<Map<?, ?>> deathInfo = plugin.getStatsConfig().getMapList("stats." + playerUUID + ".deaths_info");
        Map<?, ?> death = deathInfo.get(deathIndex);

        Inventory deathInventoryGUI = Bukkit.createInventory(null, 54, ChatColor.DARK_RED + "Death #" + (deathIndex + 1) + " Inventory");

        List<Map<String, Object>> inventory = (List<Map<String, Object>>) death.get("inventory");
        if (inventory != null) {
            // Hotbar and main inventory
            for (int i = 0; i < 36 && i < inventory.size(); i++) {
                Map<String, Object> itemMap = inventory.get(i);
                if (itemMap != null) {
                    ItemStack item = ItemStack.deserialize(itemMap);
                    deathInventoryGUI.setItem(i, item);
                }
            }

            // Armor
            if (inventory.size() > 39) {
                Map<String, Object> bootsMap = inventory.get(36);
                if (bootsMap != null) {
                    deathInventoryGUI.setItem(48, ItemStack.deserialize(bootsMap));
                }
                Map<String, Object> leggingsMap = inventory.get(37);
                if (leggingsMap != null) {
                    deathInventoryGUI.setItem(47, ItemStack.deserialize(leggingsMap));
                }
                Map<String, Object> chestplateMap = inventory.get(38);
                if (chestplateMap != null) {
                    deathInventoryGUI.setItem(46, ItemStack.deserialize(chestplateMap));
                }
                Map<String, Object> helmetMap = inventory.get(39);
                if (helmetMap != null) {
                    deathInventoryGUI.setItem(45, ItemStack.deserialize(helmetMap));
                }
            }


            // Off-hand
            if (inventory.size() > 40) {
                Map<String, Object> offhandMap = inventory.get(40);
                if (offhandMap != null) {
                    deathInventoryGUI.setItem(53, ItemStack.deserialize(offhandMap));
                }
            }
        }

        viewer.openInventory(deathInventoryGUI);
    }
}
