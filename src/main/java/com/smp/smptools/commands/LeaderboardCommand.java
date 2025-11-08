package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardCommand implements CommandExecutor {

    private final SMPTools plugin;

    public LeaderboardCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        openLeaderboardHub((Player) sender);
        return true;
    }

    private void openLeaderboardHub(Player player) {
        Inventory hubGUI = Bukkit.createInventory(null, 54, "Leaderboards");

        // General Stats
        createDisplayItem(hubGUI, Material.CLOCK, 20, ChatColor.GOLD + "Playtime", "playtime");
        createDisplayItem(hubGUI, Material.DIAMOND_SWORD, 21, ChatColor.GOLD + "Player Kills", "player_kills");
        createDisplayItem(hubGUI, Material.SKELETON_SKULL, 22, ChatColor.GOLD + "Total Deaths", "deaths");
        
        // Block Stats
        createDisplayItem(hubGUI, Material.DIAMOND_PICKAXE, 23, ChatColor.GOLD + "Blocks Broken", "blocks_broken");
        createDisplayItem(hubGUI, Material.GRASS_BLOCK, 24, ChatColor.GOLD + "Blocks Placed", "blocks_placed");

        // Ores Mined
        createDisplayItem(hubGUI, Material.COAL_ORE, 37, ChatColor.GOLD + "Coal Mined", "ores_mined.coal");
        createDisplayItem(hubGUI, Material.IRON_ORE, 38, ChatColor.GOLD + "Iron Mined", "ores_mined.iron");
        createDisplayItem(hubGUI, Material.GOLD_ORE, 39, ChatColor.GOLD + "Gold Mined", "ores_mined.gold");
        createDisplayItem(hubGUI, Material.LAPIS_LAZULI, 40, ChatColor.GOLD + "Lapis Mined", "ores_mined.lapis");
        createDisplayItem(hubGUI, Material.REDSTONE, 41, ChatColor.GOLD + "Redstone Mined", "ores_mined.redstone");
        createDisplayItem(hubGUI, Material.DIAMOND, 42, ChatColor.GOLD + "Diamonds Mined", "ores_mined.diamond");
        createDisplayItem(hubGUI, Material.EMERALD, 43, ChatColor.GOLD + "Emeralds Mined", "ores_mined.emerald");

        player.openInventory(hubGUI);
    }

    private void createDisplayItem(Inventory inv, Material material, int slot, String name, String statKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + statKey); // Store the key in lore to identify the stat
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }
}