package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomesCommand extends AbstractPlayerCommand {

    public HomesCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        String playerUUID = player.getUniqueId().toString();

        ConfigurationSection homesSection = plugin.getConfig().getConfigurationSection("homes." + playerUUID);
        if (homesSection == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("homes.no-homes", player));
            return true;
        }

        Set<String> homeNames = homesSection.getKeys(false);
        if (homeNames.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("homes.no-homes", player));
            return true;
        }

        Inventory homesGUI = Bukkit.createInventory(null, 54, plugin.getMessageManager().getMessage("homes.gui-title"));

        for (String homeName : homeNames) {
            ItemStack homeItem = new ItemStack(Material.NAME_TAG);
            ItemMeta homeMeta = homeItem.getItemMeta();
            homeMeta.displayName(Component.text(homeName, NamedTextColor.GOLD));

            ConfigurationSection home = homesSection.getConfigurationSection(homeName);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("world", home.getString("world"));
            placeholders.put("x", String.valueOf(home.getDouble("x")));
            placeholders.put("y", String.valueOf(home.getDouble("y")));
            placeholders.put("z", String.valueOf(home.getDouble("z")));

            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getMessageManager().getMessage("homes.gui-world", player, placeholders));
            lore.add(plugin.getMessageManager().getMessage("homes.gui-x", player, placeholders));
            lore.add(plugin.getMessageManager().getMessage("homes.gui-y", player, placeholders));
            lore.add(plugin.getMessageManager().getMessage("homes.gui-z", player, placeholders));
            lore.add(Component.empty());
            lore.add(plugin.getMessageManager().getMessage("homes.gui-teleport", player));
            lore.add(plugin.getMessageManager().getMessage("homes.gui-delete", player));
            homeMeta.lore(lore);

            homeItem.setItemMeta(homeMeta);
            homesGUI.addItem(homeItem);
        }

        player.openInventory(homesGUI);
        return true;
    }
}
