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
import java.util.List;
import java.util.Set;

public class HomesCommand implements CommandExecutor {

    private final SMPTools plugin;

    public HomesCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        String playerUUID = player.getUniqueId().toString();

        ConfigurationSection homesSection = plugin.getConfig().getConfigurationSection("homes." + playerUUID);
        if (homesSection == null) {
            player.sendMessage(ChatColor.RED + "You have no homes set.");
            return true;
        }

        Set<String> homeNames = homesSection.getKeys(false);
        if (homeNames.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You have no homes set.");
            return true;
        }

        Inventory homesGUI = Bukkit.createInventory(null, 54, "Your Homes");

        for (String homeName : homeNames) {
            ItemStack homeItem = new ItemStack(Material.NAME_TAG);
            ItemMeta homeMeta = homeItem.getItemMeta();
            homeMeta.setDisplayName(ChatColor.GOLD + homeName);

            ConfigurationSection home = homesSection.getConfigurationSection(homeName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "World: " + home.getString("world"));
            lore.add(ChatColor.GRAY + "X: " + home.getDouble("x"));
            lore.add(ChatColor.GRAY + "Y: " + home.getDouble("y"));
            lore.add(ChatColor.GRAY + "Z: " + home.getDouble("z"));
            lore.add("");
            lore.add(ChatColor.GREEN + "Left-click to teleport");
            lore.add(ChatColor.RED + "Right-click to delete");
            homeMeta.setLore(lore);

            homeItem.setItemMeta(homeMeta);
            homesGUI.addItem(homeItem);
        }

        player.openInventory(homesGUI);
        return true;
    }
}
