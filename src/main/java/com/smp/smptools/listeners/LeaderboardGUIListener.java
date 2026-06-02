package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.leaderboard.LeaderboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaderboardGUIListener implements Listener {

    private final SMPTools plugin;
    private final String hubTitle;
    private final String statTitlePrefix;

    public LeaderboardGUIListener(SMPTools plugin) {
        this.plugin = plugin;
        // Resolve configurable titles once at construction so renaming the
        // GUI does not require code changes.
        this.hubTitle = PlainTextComponentSerializer.plainText().serialize(
                plugin.getMessageManager().getMessage("leaderboard.gui-title"));
        this.statTitlePrefix = PlainTextComponentSerializer.plainText().serialize(
                plugin.getMessageManager().getMessage("leaderboard.stat-title", null,
                        Collections.singletonMap("title", "")));
    }

    @EventHandler
    public void onHubClick(InventoryClickEvent event) {
        if (!PlainTextComponentSerializer.plainText().serialize(event.getView().title()).equals(hubTitle)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getItemMeta().lore() == null || clickedItem.getItemMeta().lore().isEmpty()) {
            return;
        }

        String statKey = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().lore().get(0));
        openLeaderboardGUI(player, statKey);
    }

    @EventHandler
    public void onLeaderboardClick(InventoryClickEvent event) {
        if (!PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith(statTitlePrefix)) {
            return;
        }
        event.setCancelled(true);
    }

    private void openLeaderboardGUI(Player player, String statKey) {
        LeaderboardManager manager = plugin.getLeaderboardManager();
        Map<String, Long> leaderboard = manager.getLeaderboard(statKey);

        String title = statKey.replace('_', ' ').toUpperCase();
        Inventory leaderboardGUI = Bukkit.createInventory(null, 54,
                plugin.getMessageManager().getMessage("leaderboard.stat-title", player,
                        Map.of("title", title)));

        if (leaderboard.isEmpty()) {
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("leaderboard.no-data"));
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

            headMeta.displayName(plugin.getMessageManager().getMessage("leaderboard.gui-rank", player,
                    Map.of("rank", String.valueOf(rank.get()), "name", playerName)));
            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getMessageManager().getMessage("leaderboard.gui-score", player,
                    Map.of("score", formatScore(statKey, score))));
            headMeta.lore(lore);
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
