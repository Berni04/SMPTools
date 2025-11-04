package com.smp.smptools.commands;

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

import java.util.Arrays;
import java.util.List;

public class PrefixCommand implements CommandExecutor {

    public static final List<String> prefixes = Arrays.asList("❤", "♦", "⭐", "☠", "✔", "✖", "⚡", "☢");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        Inventory prefixGUI = Bukkit.createInventory(null, 9, ChatColor.DARK_PURPLE + "Choose a Prefix");

        for (int i = 0; i < prefixes.size(); i++) {
            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RESET + prefixes.get(i));
            item.setItemMeta(meta);
            prefixGUI.setItem(i, item);
        }

        player.openInventory(prefixGUI);
        return true;
    }
}
