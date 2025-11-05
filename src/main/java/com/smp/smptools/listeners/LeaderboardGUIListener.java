package com.smp.smptools.listeners;

import com.smp.smptools.commands.LeaderboardCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class LeaderboardGUIListener implements Listener {

    private final LeaderboardCommand leaderboardCommand;

    public LeaderboardGUIListener(LeaderboardCommand leaderboardCommand) {
        this.leaderboardCommand = leaderboardCommand;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.DARK_AQUA + "Leaderboard")) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        List<String> lore = event.getCurrentItem().getItemMeta().getLore();

        if (lore != null && !lore.isEmpty()) {
            String stat = lore.get(0);
            leaderboardCommand.openLeaderboardGUI(player, stat);
        }
    }
}
