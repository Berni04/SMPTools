package com.smp.smptools.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CustomItemCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("smptools.customitem")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(Component.text("Usage: /customitem <item_type> <custom_model_data>", NamedTextColor.RED));
            return true;
        }

        Material material;
        try {
            material = Material.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid item type: " + args[0], NamedTextColor.RED));
            return true;
        }

        int customModelData;
        try {
            customModelData = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid custom model data: " + args[1] + ". Must be an integer.", NamedTextColor.RED));
            return true;
        }

        ItemStack customItem = new ItemStack(material);
        ItemMeta meta = customItem.getItemMeta();
        meta.setCustomModelData(customModelData);
        customItem.setItemMeta(meta);

        player.getInventory().addItem(customItem);
        player.sendMessage(Component.text("Gave you a " + material.name() + " with CustomModelData " + customModelData, NamedTextColor.GREEN));

        return true;
    }
}
