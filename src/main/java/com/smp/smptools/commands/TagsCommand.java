package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TagsCommand extends AbstractPlayerCommand {

    public TagsCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (args.length == 0) {
            openTagsGUI(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!player.hasPermission("smptools.tags.set")) {
                player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                        java.util.Map.of("usage", "/tags set <player> <title>")));
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore()) {
                player.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
                return true;
            }

            if (!target.isOnline()) {
                player.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
                return true;
            }

            String title = "";
            for (int i = 2; i < args.length; i++) {
                title += args[i] + " ";
            }
            title = title.trim();

            plugin.getTagManager().unlockTitle((Player) target, title);
            player.sendMessage(plugin.getMessageManager().getMessage("tags.unlocked", player,
                    java.util.Map.of("title", title, "target", args[1])));
            return true;
        }

        player.sendMessage(plugin.getMessageManager().getMessage("common.unknown-subcommand", player,
                java.util.Map.of("usage", "/tags [set <player> <title>]")));
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
                    lore.add(plugin.getMessageManager().getMessage("tags.gui-unlocked", player));
                    meta.lore(lore);
                } else {
                    meta.displayName(Component.text(title, NamedTextColor.RED));
                    List<Component> lore = new ArrayList<>();
                    lore.add(plugin.getMessageManager().getMessage("tags.gui-how-to-unlock", player,
                            Map.of("description", description != null ? description : "")));
                    meta.lore(lore);
                }

                item.setItemMeta(meta);
                tagsGUI.addItem(item);
            }
        }

        ItemStack clearItem = new ItemStack(Material.BARRIER);
        ItemMeta clearMeta = clearItem.getItemMeta();
        clearMeta.displayName(Component.text("Clear Title", NamedTextColor.RED));
        clearItem.setItemMeta(clearMeta);
        tagsGUI.setItem(53, clearItem);

        player.openInventory(tagsGUI);
    }
}
