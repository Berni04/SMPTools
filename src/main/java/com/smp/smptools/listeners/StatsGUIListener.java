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

public class StatsGUIListener implements Listener {

    private final StatsCommand statsCommand;

    public StatsGUIListener(StatsCommand statsCommand) {
        this.statsCommand = statsCommand;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("'s Stats")) {
            event.setCancelled(true);

            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                Player player = (Player) event.getWhoClicked();
                String targetName = ChatColor.stripColor(title).replace("'s Stats", "");
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                statsCommand.showDeathInfoGUI(player, target);
            }
        } else if (title.contains("'s Deaths")) {
            event.setCancelled(true);
        }
    }
}
