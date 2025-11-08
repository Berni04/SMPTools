package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.PrefixCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PrefixGUIListener implements Listener {

    private final SMPTools plugin;

    public PrefixGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Choose a Prefix")) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() != Material.NAME_TAG) {
            return;
        }

        String prefix = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (PrefixCommand.prefixes.contains(prefix)) {
            plugin.getStatsConfig().set("players." + player.getUniqueId() + ".prefix", prefix);
            plugin.saveStatsConfig();
            plugin.getNameTagListener().updatePlayerName(player);
            player.sendMessage(ChatColor.GREEN + "Your prefix has been set to: " + prefix);
            player.closeInventory();
        }
    }
}
