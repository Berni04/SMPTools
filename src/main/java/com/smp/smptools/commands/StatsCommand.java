package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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
import java.util.List;
import java.util.Map;

public class StatsCommand implements CommandExecutor {

    private final SMPTools plugin;

    public StatsCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    public SMPTools getPlugin() {
        return plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("You must specify a player to view their stats.", NamedTextColor.RED));
                return true;
            }
            showStatsGUI((Player) sender, (Player) sender);
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }
            showStatsGUI((Player) sender, target);
        }
        return true;
    }

    private void showStatsGUI(Player viewer, OfflinePlayer target) {
        String playerUUID = target.getUniqueId().toString();
        ConfigurationSection statsSection = plugin.getStatsConfig().getConfigurationSection("stats." + playerUUID);

        Inventory statsGUI = Bukkit.createInventory(null, 54, Component.text(target.getName() + "'s Stats", TextColor.fromHexString("#008B8B")));

        // Player Head and General Info
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        headMeta.setOwningPlayer(target);
        headMeta.displayName(Component.text(target.getName(), NamedTextColor.GOLD));
        playerHead.setItemMeta(headMeta);
        statsGUI.setItem(4, playerHead);

        // General Stats
        long playtimeMinutes;
        if (target.isOnline()) {
            playtimeMinutes = ((Player) target).getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / (20 * 60);
        } else {
            playtimeMinutes = statsSection != null ? statsSection.getLong("playtime_minutes", 0) : 0;
        }
        createDisplayItem(statsGUI, Material.CLOCK, 20, Component.text("Playtime", NamedTextColor.GOLD),
                Component.text(formatPlaytime(playtimeMinutes), NamedTextColor.YELLOW));
        createDisplayItem(statsGUI, Material.DIAMOND_SWORD, 21, Component.text("Player Kills", NamedTextColor.GOLD),
                Component.text(String.valueOf(statsSection != null ? statsSection.getInt("player_kills", 0) : 0), NamedTextColor.YELLOW));
        createDisplayItem(statsGUI, Material.SKELETON_SKULL, 22, Component.text("Total Deaths", NamedTextColor.GOLD),
                Component.text(String.valueOf(statsSection != null ? statsSection.getInt("deaths_total", 0) : 0), NamedTextColor.YELLOW));

        // Block Stats
        createDisplayItem(statsGUI, Material.DIAMOND_PICKAXE, 23, Component.text("Blocks Broken", NamedTextColor.GOLD),
                Component.text(String.valueOf(statsSection != null ? statsSection.getInt("blocks_broken", 0) : 0), NamedTextColor.YELLOW));
        createDisplayItem(statsGUI, Material.GRASS_BLOCK, 24, Component.text("Blocks Placed", NamedTextColor.GOLD),
                Component.text(String.valueOf(statsSection != null ? statsSection.getInt("blocks_placed", 0) : 0), NamedTextColor.YELLOW));

        // Ores Mined
        createDisplayItem(statsGUI, Material.COAL_ORE, 37, Component.text("Coal Mined", NamedTextColor.GOLD),
                Component.text(String.valueOf(statsSection != null ? statsSection.getInt("ores_mined.coal", 0) : 0), NamedTextColor.YELLOW));
        createDisplayItem(statsGUI, Material.IRON_ORE, 38, Component.text("Iron Mined", NamedTextColor.GOLD),
                Component.text(String.valueOf(statsSection != null ? statsSection.getInt("ores_mined.iron", 0) : 0), NamedTextColor.YELLOW));
        createDisplayItem(statsGUI, Material.GOLD_ORE, 39, Component.text("Gold Mined", NamedTextColor.GOLD),
                Component.text(String.valueOf(statsSection != null ? statsSection.getInt("ores_mined.gold", 0) : 0), NamedTextColor.YELLOW));
        createDisplayItem(statsGUI, Material.LAPIS_LAZULI, 40, Component.text("Lapis Mined", NamedTextColor.GOLD),
                Component.text(String.valueOf(statsSection != null ? statsSection.getInt("ores_mined.lapis", 0) : 0), NamedTextColor.YELLOW));
        createDisplayItem(statsGUI, Material.REDSTONE, 41, Component.text("Redstone Mined", NamedTextColor.GOLD),
                Component.text(String.valueOf(statsSection != null ? statsSection.getInt("ores_mined.redstone", 0) : 0), NamedTextColor.YELLOW));
        createDisplayItem(statsGUI, Material.DIAMOND, 42, Component.text("Diamonds Mined", NamedTextColor.GOLD),
                Component.text(String.valueOf(statsSection != null ? statsSection.getInt("ores_mined.diamond", 0) : 0), NamedTextColor.YELLOW));
        createDisplayItem(statsGUI, Material.EMERALD, 43, Component.text("Emeralds Mined", NamedTextColor.GOLD),
                Component.text(String.valueOf(statsSection != null ? statsSection.getInt("ores_mined.emerald", 0) : 0), NamedTextColor.YELLOW));

        // Deaths Button
        createDisplayItem(statsGUI, Material.PAPER, 49, Component.text("View Deaths", NamedTextColor.RED),
                Component.text("Click to see detailed death info", NamedTextColor.YELLOW));

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

    private void createDisplayItem(Inventory inv, Material material, int slot, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        List<Component> loreList = new ArrayList<>();
        for (Component c : lore) {
            loreList.add(c);
        }
        meta.lore(loreList);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    public void showDeathInfoGUI(Player viewer, OfflinePlayer target) {
        String playerUUID = target.getUniqueId().toString();
        List<Map<?, ?>> deathInfo = plugin.getStatsConfig().getMapList("stats." + playerUUID + ".deaths_info");

        Inventory deathInfoGUI = Bukkit.createInventory(null, 54, Component.text(target.getName() + "'s Deaths", TextColor.fromHexString("#008B8B")));

        for (int i = 0; i < deathInfo.size(); i++) {
            Map<?, ?> death = deathInfo.get(i);
            String time = (String) death.get("time");
            String cause = (String) death.get("cause");

            createDisplayItem(deathInfoGUI, Material.PAPER, i, Component.text("Death #" + (i + 1), NamedTextColor.RED),
                    Component.text("Time: ", NamedTextColor.YELLOW).append(Component.text(time != null ? time : "Unknown", NamedTextColor.WHITE)),
                    Component.text("Cause: ", NamedTextColor.YELLOW).append(Component.text(cause != null ? cause : "Unknown", NamedTextColor.WHITE)),
                    Component.text("Click for more info", NamedTextColor.GRAY));
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
            case "coal": return Material.COAL;
            case "lapis": return Material.LAPIS_LAZULI;
            case "redstone": return Material.REDSTONE;
            case "emerald": return Material.EMERALD;
            case "copper": return Material.COPPER_INGOT;
            case "quartz": return Material.QUARTZ;
            case "netherite": return Material.NETHERITE_SCRAP;
            default: return Material.STONE;
        }
    }

    public void showDetailedDeathInfoGUI(Player viewer, OfflinePlayer target, int deathIndex) {
        String playerUUID = target.getUniqueId().toString();
        List<Map<?, ?>> deathInfo = plugin.getStatsConfig().getMapList("stats." + playerUUID + ".deaths_info");

        if (deathIndex < 0 || deathIndex >= deathInfo.size()) {
            viewer.sendMessage(Component.text("Invalid death index.", NamedTextColor.RED));
            return;
        }

        Map<?, ?> death = deathInfo.get(deathIndex);
        String time = (String) death.get("time");
        String cause = (String) death.get("cause");
        Map<?, ?> location = (Map<?, ?>) death.get("location");
        String world = location != null ? (String) location.get("world") : "Unknown";
        double x = location != null ? ((Number) location.get("x")).doubleValue() : 0;
        double y = location != null ? ((Number) location.get("y")).doubleValue() : 0;
        double z = location != null ? ((Number) location.get("z")).doubleValue() : 0;

        Inventory detailedDeathInfoGUI = Bukkit.createInventory(null, 54,
                Component.text(target.getName() + "'s Death #" + (deathIndex + 1), TextColor.fromHexString("#8B0000")));

        createDisplayItem(detailedDeathInfoGUI, Material.CLOCK, 10, Component.text("Time", NamedTextColor.YELLOW),
                Component.text(time != null ? time : "Unknown", NamedTextColor.WHITE));
        createDisplayItem(detailedDeathInfoGUI, Material.COMPASS, 12, Component.text("Coordinates", NamedTextColor.YELLOW),
                Component.text("X: " + (int) x + ", Y: " + (int) y + ", Z: " + (int) z, NamedTextColor.WHITE));
        createDisplayItem(detailedDeathInfoGUI, Material.SKELETON_SKULL, 14, Component.text("Cause", NamedTextColor.YELLOW),
                Component.text(cause != null ? cause : "Unknown", NamedTextColor.WHITE));
        createDisplayItem(detailedDeathInfoGUI, Material.CHEST, 16, Component.text("Inventory", NamedTextColor.YELLOW),
                Component.text("Click to view", NamedTextColor.GRAY));

        viewer.openInventory(detailedDeathInfoGUI);
    }

    public void showDeathInventoryGUI(Player viewer, OfflinePlayer target, int deathIndex) {
        String playerUUID = target.getUniqueId().toString();
        List<Map<?, ?>> deathInfo = plugin.getStatsConfig().getMapList("stats." + playerUUID + ".deaths_info");

        if (deathIndex < 0 || deathIndex >= deathInfo.size()) {
            viewer.sendMessage(Component.text("Invalid death index.", NamedTextColor.RED));
            return;
        }

        Map<?, ?> death = deathInfo.get(deathIndex);
        List<Map<?, ?>> inventoryData = (List<Map<?, ?>>) death.get("inventory");

        Inventory deathInventoryGUI = Bukkit.createInventory(null, 54,
                Component.text(target.getName() + "'s Death #" + (deathIndex + 1) + " Inventory", TextColor.fromHexString("#8B0000")));

        if (inventoryData != null) {
            for (int i = 0; i < inventoryData.size() && i < 54; i++) {
                Map<?, ?> itemData = inventoryData.get(i);
                if (itemData != null) {
                    String materialName = (String) itemData.get("material");
                    int amount = itemData.containsKey("amount") ? ((Number) itemData.get("amount")).intValue() : 1;
                    if (materialName != null) {
                        try {
                            Material material = Material.valueOf(materialName);
                            ItemStack item = new ItemStack(material, amount);
                            deathInventoryGUI.setItem(i, item);
                        } catch (IllegalArgumentException e) {
                            // Invalid material, skip
                        }
                    }
                }
            }
        }

        // Rollback button
        if (viewer.hasPermission("smptools.stats.rollback")) {
            ItemStack rollbackItem = new ItemStack(Material.ANVIL);
            ItemMeta rollbackMeta = rollbackItem.getItemMeta();
            rollbackMeta.displayName(Component.text("Rollback Inventory", NamedTextColor.GREEN));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to restore this inventory", NamedTextColor.GRAY));
            lore.add(Component.text("to the player.", NamedTextColor.GRAY));
            rollbackMeta.lore(lore);
            rollbackItem.setItemMeta(rollbackMeta);
            deathInventoryGUI.setItem(50, rollbackItem);
        }

        viewer.openInventory(deathInventoryGUI);
    }
}
