package com.smp.smptools.listeners;

import com.smp.smptools.commands.StatsCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class StatsGUIListener implements Listener {

    private final StatsCommand statsCommand;

    public StatsGUIListener(StatsCommand statsCommand) {
        this.statsCommand = statsCommand;
    }

    @EventHandler
    public void onStatsGUIClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("'s Stats")) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null) {
            return;
        }

        if (clickedItem.getType() == Material.PAPER && clickedItem.getItemMeta().getDisplayName().contains("View Deaths")) {
            statsCommand.showDeathInfoGUI(player, Bukkit.getOfflinePlayer(event.getView().getTitle().split("'")[0].replace(ChatColor.DARK_AQUA.toString(), "")));
        }
    }
}
