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

    public static final class TradeRequest {
        private final UUID senderUUID;
        private final long requestId;

        public TradeRequest(UUID senderUUID, long requestId) {
            this.senderUUID = senderUUID;
            this.requestId = requestId;
        }

        public UUID getSenderUUID() {
            return senderUUID;
        }

        public long getRequestId() {
            return requestId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TradeRequest that = (TradeRequest) o;
            return requestId == that.requestId && java.util.Objects.equals(senderUUID, that.senderUUID);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(senderUUID, requestId);
        }
    }

    private final SMPTools plugin;
    // Map of target UUID -> TradeRequest
    final Map<UUID, TradeRequest> pendingRequests = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong requestIdGenerator = new java.util.concurrent.atomic.AtomicLong();
    // Map of active trade sessions (player UUID -> TradeSession)
    private final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();

    public TradeManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player sender, Player target) {
        if (sender.equals(target)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("trade.cannot-trade-self", sender));
            return;
        }

        if (isTrading(sender) || isTrading(target)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("trade.already-trading", sender));
            return;
        }

        if (pendingRequests.containsKey(target.getUniqueId())) {
            sender.sendMessage(plugin.getMessageManager().getMessage("trade.already-trading", sender));
            return;
        }

        long requestId = requestIdGenerator.incrementAndGet();
        TradeRequest request = new TradeRequest(sender.getUniqueId(), requestId);
        pendingRequests.put(target.getUniqueId(), request);

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
            if (pendingRequests.remove(target.getUniqueId(), request)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("trade.expired", sender, java.util.Map.of("target", target.getName())));
            }
        }, timeout * 20L);
    }

    public void acceptRequest(Player target) {
        TradeRequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(plugin.getMessageManager().getMessage("trade.no-request"));
            return;
        }

        UUID senderUUID = request.getSenderUUID();
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
        TradeRequest request = pendingRequests.remove(target.getUniqueId());
        if (request != null) {
            UUID senderUUID = request.getSenderUUID();
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

    public void cleanupPendingRequests(UUID playerUUID) {
        if (playerUUID == null) return;
        pendingRequests.remove(playerUUID);
        pendingRequests.values().removeIf(req -> req.getSenderUUID().equals(playerUUID));
    }

    public void cleanup() {
        java.util.Set<TradeSession> sessionsToCancel = new java.util.HashSet<>(activeSessions.values());
        for (TradeSession session : sessionsToCancel) {
            Player p1 = session.getPlayer1();
            Player p2 = session.getPlayer2();
            session.cancelTrade(
                plugin.getMessageManager().getMessage("trade.server-shutdown", p1),
                plugin.getMessageManager().getMessage("trade.server-shutdown", p2)
            );
        }
        activeSessions.clear();
        pendingRequests.clear();
    }
}
