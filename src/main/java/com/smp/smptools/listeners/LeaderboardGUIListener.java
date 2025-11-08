package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.leaderboard.LeaderboardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaderboardGUIListener implements Listener {

    private final SMPTools plugin;

    public LeaderboardGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHubClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Leaderboards")) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getItemMeta().getLore() == null || clickedItem.getItemMeta().getLore().isEmpty()) {
            return;
        }

        String statKey = ChatColor.stripColor(clickedItem.getItemMeta().getLore().get(0));
        openLeaderboardGUI(player, statKey);
    }

    @EventHandler
    public void onLeaderboardClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("Top 10 -")) {
            return;
        }
        event.setCancelled(true);
    }

    private void openLeaderboardGUI(Player player, String statKey) {
        LeaderboardManager manager = plugin.getLeaderboardManager();
        Map<String, Long> leaderboard = manager.getLeaderboard(statKey);

        String title = statKey.replace('_', ' ').toUpperCase();
        Inventory leaderboardGUI = Bukkit.createInventory(null, 54, "Top 10 - " + title);

        if (leaderboard.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No leaderboard data available for this stat.");
            return;
        }

        AtomicInteger rank = new AtomicInteger(1);
        leaderboard.forEach((playerName, score) -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();

            if (offlinePlayer != null) {
                headMeta.setOwningPlayer(offlinePlayer);
            }

            headMeta.setDisplayName(ChatColor.GOLD + "#" + rank.get() + " " + playerName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Score: " + formatScore(statKey, score));
            headMeta.setLore(lore);
            playerHead.setItemMeta(headMeta);
            leaderboardGUI.addItem(playerHead);
            rank.getAndIncrement();
        });

        player.openInventory(leaderboardGUI);
    }

    private String formatScore(String statType, long score) {
        if (statType.equals("playtime")) {
            long days = score / 1440;
            long hours = (score % 1440) / 60;
            long minutes = score % 60;
            return String.format("%dD %dH %dM", days, hours, minutes);
        }
        return String.valueOf(score);
    }
}
