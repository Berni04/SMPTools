package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener implements Listener {

    private final SMPTools plugin;

    public JoinLeaveListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(SMPTools.getInstance().getMessageManager().getMessage("join-leave.joined", player));
        if (plugin.getMissionManager() != null) {
            plugin.getMissionManager().claimPendingMissionRewards(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.quitMessage(SMPTools.getInstance().getMessageManager().getMessage("join-leave.left", player));

        long totalTicks = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
        long totalMinutes = totalTicks / (20 * 60);
        plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".playtime_minutes", totalMinutes);
        if (plugin.getStorageManager() != null && plugin.getStorageManager().getProvider() != null) {
            plugin.getStorageManager().getProvider().saveStat(player.getUniqueId(), "playtime_minutes", totalMinutes);
        }
        if (plugin.getChatManager() != null) {
            plugin.getChatManager().removePlayer(player.getUniqueId());
        }
        if (plugin.getConfig().getBoolean("stats.save-on-quit", true)) {
            plugin.saveStatsConfig();
        }
    }
}
