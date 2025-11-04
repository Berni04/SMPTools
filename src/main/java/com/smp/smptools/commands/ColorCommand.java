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

import java.util.ArrayList;
import java.util.List;

public class ColorCommand implements CommandExecutor {

    public static final List<ChatColor> colors = new ArrayList<>();

    static {
        for (ChatColor color : ChatColor.values()) {
            if (color.isColor()) {
                colors.add(color);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        Inventory colorGUI = Bukkit.createInventory(null, 18, ChatColor.DARK_PURPLE + "Choose a Color");

        for (ChatColor color : colors) {
            Material woolMaterial = getWoolColor(color);
            ItemStack item = new ItemStack(woolMaterial);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(color + color.name());
            item.setItemMeta(meta);
            colorGUI.addItem(item);
        }

        player.openInventory(colorGUI);
        return true;
    }

    private Material getWoolColor(ChatColor color) {
        switch (color) {
            case BLACK: return Material.BLACK_WOOL;
            case DARK_BLUE: return Material.BLUE_WOOL;
            case DARK_GREEN: return Material.GREEN_WOOL;
            case DARK_AQUA: return Material.CYAN_WOOL;
            case DARK_RED: return Material.RED_WOOL;
            case DARK_PURPLE: return Material.PURPLE_WOOL;
            case GOLD: return Material.ORANGE_WOOL;
            case GRAY: return Material.LIGHT_GRAY_WOOL;
            case DARK_GRAY: return Material.GRAY_WOOL;
            case BLUE: return Material.LIGHT_BLUE_WOOL;
            case GREEN: return Material.LIME_WOOL;
            case AQUA: return Material.LIGHT_BLUE_WOOL;
            case RED: return Material.RED_WOOL;
            case LIGHT_PURPLE: return Material.MAGENTA_WOOL;
            case YELLOW: return Material.YELLOW_WOOL;
            case WHITE: return Material.WHITE_WOOL;
            default: return Material.WHITE_WOOL;
        }
    }
}
