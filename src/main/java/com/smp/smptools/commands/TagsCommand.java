package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

import java.util.ArrayList;
import java.util.List;

public class TagsCommand implements CommandExecutor {

    private final SMPTools plugin;

    public TagsCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            openTagsGUI(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!player.hasPermission("smptools.tags.set")) {
                player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(Component.text("Usage: /tags set <player> <title>", NamedTextColor.RED));
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore()) {
                player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }

            String title = "";
            for (int i = 2; i < args.length; i++) {
                title += args[i] + " ";
            }
            title = title.trim();

            plugin.getTagManager().unlockTitle((Player) target, title);
            player.sendMessage(Component.text("Title '" + title + "' unlocked for " + target.getName(), NamedTextColor.GREEN));
            return true;
        }

        player.sendMessage(Component.text("Unknown subcommand. Usage: /tags [set <player> <title>]", NamedTextColor.RED));
        return true;
    }

    private void openTagsGUI(Player player) {
        List<String> unlockedTitles = plugin.getTagManager().getUnlockedTitles(player);
        Inventory tagsGUI = Bukkit.createInventory(null, 54, "Your Titles");

        ConfigurationSection milestones = plugin.getTagsConfig().getConfigurationSection("milestones");
        if (milestones != null) {
            for (String key : milestones.getKeys(false)) {
                ConfigurationSection milestone = milestones.getConfigurationSection(key);
                if (milestone == null) continue;

                String title = milestone.getString("title");
                String description = milestone.getString("description");
                if (title == null) continue;

                boolean unlocked = unlockedTitles.contains(title);

                ItemStack item = new ItemStack(unlocked ? Material.LIME_DYE : Material.GRAY_DYE);
                ItemMeta meta = item.getItemMeta();

                if (unlocked) {
                    meta.displayName(Component.text(title, NamedTextColor.GOLD));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text(description != null ? description : "", NamedTextColor.GRAY));
                    lore.add(Component.text("Unlocked!", NamedTextColor.GREEN));
                    meta.lore(lore);
                } else {
                    meta.displayName(Component.text(title, NamedTextColor.RED));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("How to unlock: " + (description != null ? description : ""), NamedTextColor.GRAY));
                    meta.lore(lore);
                }

                item.setItemMeta(meta);
                tagsGUI.addItem(item);
            }
        }

        // Clear title button
        ItemStack clearItem = new ItemStack(Material.BARRIER);
        ItemMeta clearMeta = clearItem.getItemMeta();
        clearMeta.displayName(Component.text("Clear Title", NamedTextColor.RED));
        clearItem.setItemMeta(clearMeta);
        tagsGUI.setItem(53, clearItem);

        player.openInventory(tagsGUI);
    }
}
