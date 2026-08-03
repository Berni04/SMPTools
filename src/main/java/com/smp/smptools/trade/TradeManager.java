package com.smp.smptools.trade;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    private final SMPTools plugin;
    // Map of target UUID -> requester UUID
    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();
    // Map of active trade sessions (player UUID -> TradeSession)
    private final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();

    public TradeManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player sender, Player target) {
        if (sender.equals(target)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("trade.cannot-trade-self"));
            return;
        }

        if (isTrading(sender) || isTrading(target)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("trade.already-trading"));
            return;
        }

        pendingRequests.put(target.getUniqueId(), sender.getUniqueId());

        Component acceptBtn = Component.text("[Accept]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/trade accept"));
        Component denyBtn = Component.text("[Deny]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/trade deny"));

        sender.sendMessage(plugin.getMessageManager().getMessage("trade.request-sent", sender, java.util.Map.of("target", target.getName())));

        Component msg = plugin.getMessageManager().getMessage("trade.request-received", target, java.util.Map.of("player", sender.getName()));
        target.sendMessage(msg
                .append(Component.text(" "))
                .append(acceptBtn)
                .append(Component.text(" "))
                .append(denyBtn));

        int timeout = plugin.getConfig().getInt("features.remote-trade.request-timeout-seconds", 60);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.remove(target.getUniqueId(), sender.getUniqueId())) {
                sender.sendMessage(plugin.getMessageManager().getMessage("trade.expired", sender, java.util.Map.of("target", target.getName())));
            }
        }, timeout * 20L);
    }

    public void acceptRequest(Player target) {
        UUID senderUUID = pendingRequests.remove(target.getUniqueId());
        if (senderUUID == null) {
            target.sendMessage(plugin.getMessageManager().getMessage("trade.no-request"));
            return;
        }

        Player sender = Bukkit.getPlayer(senderUUID);
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
            return;
        }

        if (isTrading(sender) || isTrading(target)) {
            target.sendMessage(plugin.getMessageManager().getMessage("trade.already-trading"));
            return;
        }

        TradeSession session = new TradeSession(plugin, sender, target);
        activeSessions.put(sender.getUniqueId(), session);
        activeSessions.put(target.getUniqueId(), session);

        session.open();
    }

    public void denyRequest(Player target) {
        UUID senderUUID = pendingRequests.remove(target.getUniqueId());
        if (senderUUID != null) {
            Player sender = Bukkit.getPlayer(senderUUID);
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(plugin.getMessageManager().getMessage("trade.cancelled"));
            }
            target.sendMessage(plugin.getMessageManager().getMessage("trade.cancelled"));
        } else {
            target.sendMessage(plugin.getMessageManager().getMessage("trade.no-request"));
        }
    }

    public boolean isTrading(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public TradeSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public void removeSession(UUID p1, UUID p2) {
        activeSessions.remove(p1);
        activeSessions.remove(p2);
    }
}
