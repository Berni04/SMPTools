package com.smp.smptools.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        Inventory prefixGUI = Bukkit.createInventory(null, 9, Component.text("Choose a Prefix", TextColor.fromHexString("#5B2C6F")));

        for (int i = 0; i < prefixes.size(); i++) {
            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(prefixes.get(i)));
            item.setItemMeta(meta);
            prefixGUI.setItem(i, item);
        }

        player.openInventory(prefixGUI);
        return true;
    }
}
