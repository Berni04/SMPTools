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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaderboardGUIListener implements Listener {

    private final SMPTools plugin;
    private final String hubTitle;

    public LeaderboardGUIListener(SMPTools plugin) {
        this.plugin = plugin;
        // Resolve the configurable hub title for fallback compatibility
        // with other plugins that may open the same GUI without using
        // our InventoryHolder.
        this.hubTitle = PlainTextComponentSerializer.plainText().serialize(
                plugin.getMessageManager().getMessage("leaderboard.gui-title"));
    }

    @EventHandler
    public void onHubClick(InventoryClickEvent event) {
        // Primary check: our InventoryHolder. This is the only reliable
        // way to recognise the hub regardless of the configured title.
        if (!(event.getInventory().getHolder() instanceof LeaderboardHubHolder)) {
            // Fallback: legacy title-based check (other plugins may open
            // a similar GUI without our holder).
            if (!PlainTextComponentSerializer.plainText().serialize(event.getView().title()).equals(hubTitle)) {
                return;
            }
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
        // Primary check: our InventoryHolder. This is robust against any
        // customisation of the leaderboard.stat-title template.
        if (event.getInventory().getHolder() instanceof LeaderboardStatHolder) {
            event.setCancelled(true);
            return;
        }

        // Fallback: prefix-based check for compatibility with title-based
        // detection (e.g. the {title} placeholder may have been empty or
        // the GUI was opened by a third-party tool).
        if (!PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Top 10 -")) {
            return;
        }
        event.setCancelled(true);
    }

    private void openLeaderboardGUI(Player player, String statKey) {
        LeaderboardManager manager = plugin.getLeaderboardManager();
        Map<String, Long> leaderboard = manager.getLeaderboard(statKey);

        String title = statKey.replace('_', ' ').toUpperCase();
        LeaderboardStatHolder holder = new LeaderboardStatHolder(statKey);
        Inventory leaderboardGUI = Bukkit.createInventory(holder, 54,
                plugin.getMessageManager().getMessage("leaderboard.stat-title", player,
                        Map.of("title", title)));
        holder.setInventory(leaderboardGUI);

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
