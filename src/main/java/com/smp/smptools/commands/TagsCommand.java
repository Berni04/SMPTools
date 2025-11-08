package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            openTagsGUI(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!player.hasPermission("smptools.tags.set")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /tags set <player> <title>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore()) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            String title = "";
            for (int i = 2; i < args.length; i++) {
                title += args[i] + " ";
            }
            title = title.trim();

            plugin.getTagManager().unlockTitle((Player) target, title);
            player.sendMessage(ChatColor.GREEN + "Title '" + title + "' unlocked for " + target.getName());
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /tags [set <player> <title>]");
        return true;
    }

    private void openTagsGUI(Player player) {
        List<String> unlockedTitles = plugin.getTagManager().getUnlockedTitles(player);
        Inventory tagsGUI = Bukkit.createInventory(null, 54, "Your Titles");

        ConfigurationSection milestones = plugin.getTagsConfig().getConfigurationSection("milestones");
        if (milestones != null) {
            for (String key : milestones.getKeys(false)) {
                ConfigurationSection milestone = milestones.getConfigurationSection(key);
                String title = milestone.getString("title");
                String description = milestone.getString("description");

                if (unlockedTitles.contains(title)) {
                    // Unlocked
                    ItemStack titleItem = new ItemStack(Material.NAME_TAG);
                    ItemMeta meta = titleItem.getItemMeta();
                    meta.setDisplayName(ChatColor.GOLD + title);
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GREEN + "Unlocked!");
                    meta.setLore(lore);
                    titleItem.setItemMeta(meta);
                    tagsGUI.addItem(titleItem);
                } else {
                    // Locked
                    ItemStack titleItem = new ItemStack(Material.BARRIER);
                    ItemMeta meta = titleItem.getItemMeta();
                    meta.setDisplayName(ChatColor.RED + title);
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "How to unlock: " + description);
                    meta.setLore(lore);
                    titleItem.setItemMeta(meta);
                    tagsGUI.addItem(titleItem);
                }
            }
        }

        // Add an item to clear the current title
        ItemStack clearItem = new ItemStack(Material.BARRIER);
        ItemMeta clearMeta = clearItem.getItemMeta();
        clearMeta.setDisplayName(ChatColor.RED + "Clear Title");
        clearItem.setItemMeta(clearMeta);
        tagsGUI.setItem(53, clearItem); // Last slot

        player.openInventory(tagsGUI);
    }
}
