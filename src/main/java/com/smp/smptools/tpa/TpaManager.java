package com.smp.smptools.tpa;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player teleportation requests (TPA system).
 * Handles sending, accepting, denying, and toggling teleport requests
 * with automatic timeout functionality.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Thread-safe request tracking using ConcurrentHashMap</li>
 *   <li>Automatic request timeout after 60 seconds</li>
 *   <li>Toggleable request acceptance per player</li>
 *   <li>Clickable accept/deny buttons in chat</li>
 * </ul>
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public class TpaManager {

    private final SMPTools plugin;
    /** Map of pending requests: Target UUID -> Requester UUID */
    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();
    /** Set of players who have toggled off TPA requests */
    private final Set<UUID> tpaToggledOff = ConcurrentHashMap.newKeySet();

    /**
     * Constructs a new TpaManager.
     *
     * @param plugin the SMPTools plugin instance
     */
    public TpaManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends a teleport request from the requester to the target player.
     * The request will timeout after {@link Constants#TPA_TIMEOUT_SECONDS} seconds.
     *
     * @param requester the player requesting to teleport
     * @param target the player to teleport to
     */
    public void sendTeleportRequest(@NotNull Player requester, @NotNull Player target) {
        if (plugin.getKrampusManager() != null && plugin.getKrampusManager().isKidnapped(requester)) {
            requester.sendMessage(plugin.getMessageManager().getMessage("tpa.caged", requester));
            return;
        }
        if (requester.isDead() || target.isDead()) {
            requester.sendMessage(plugin.getMessageManager().getMessage("common.player-dead", requester));
            return;
        }

        if (tpaToggledOff.contains(target.getUniqueId())) {
            requester.sendMessage(plugin.getMessageManager().getMessage("tpa.target-toggled-off", requester,
                    Map.of("target", target.getName())));
            return;
        }

        if (pendingRequests.containsKey(target.getUniqueId())) {
            requester.sendMessage(plugin.getMessageManager().getMessage("tpa.already-pending", requester,
                    Map.of("target", target.getName())));
            return;
        }

        pendingRequests.put(target.getUniqueId(), requester.getUniqueId());

        requester.sendMessage(plugin.getMessageManager().getMessage("tpa.request-sent", requester,
                Map.of("target", target.getName())));

        Component acceptButton = Component.text("[Accept]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/tpa"));
        Component denyButton = Component.text("[Deny]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/tpd"));

        target.sendMessage(plugin.getMessageManager().getMessage("tpa.request-received", target,
                Map.of(), requester)
                .append(Component.text(" "))
                .append(acceptButton)
                .append(Component.text(" "))
                .append(denyButton));

        // Timeout the request after 60 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRequests.containsKey(target.getUniqueId()) && pendingRequests.get(target.getUniqueId()).equals(requester.getUniqueId())) {
                    pendingRequests.remove(target.getUniqueId());
                    if (requester.isOnline()) {
                        requester.sendMessage(plugin.getMessageManager().getMessage("tpa.expired", requester,
                                Map.of("target", target.getName())));
                    }
                    if (target.isOnline()) {
                        target.sendMessage(plugin.getMessageManager().getMessage("tpa.expired-other", target,
                                Map.of("target", requester.getName())));
                    }
                }
            }
        }.runTaskLater(plugin, Constants.TPA_TIMEOUT_TICKS);
    }

    /**
     * Accepts a pending teleport request for the given player.
     * The requester will begin teleporting to the acceptor's location.
     *
     * @param acceptor the player accepting the request
     */
    public void acceptTeleportRequest(@NotNull Player acceptor) {
        if (!pendingRequests.containsKey(acceptor.getUniqueId())) {
            acceptor.sendMessage(plugin.getMessageManager().getMessage("tpa.no-pending"));
            return;
        }

        UUID requesterUUID = pendingRequests.remove(acceptor.getUniqueId());
        Player requester = plugin.getServer().getPlayer(requesterUUID);

        if (requester == null || !requester.isOnline()) {
            acceptor.sendMessage(plugin.getMessageManager().getMessage("tpa.target-offline"));
            return;
        }

        if (plugin.getKrampusManager() != null && plugin.getKrampusManager().isKidnapped(requester)) {
            acceptor.sendMessage(plugin.getMessageManager().getMessage("tpa.target-caged", acceptor));
            requester.sendMessage(plugin.getMessageManager().getMessage("tpa.caged", requester));
            return;
        }

        plugin.getTeleportManager().startTeleport(requester, acceptor.getLocation(), "to " + acceptor.getName());
        acceptor.sendMessage(plugin.getMessageManager().getMessage("tpa.accepted", acceptor,
                Map.of(), requester));
    }

    /**
     * Denies a pending teleport request for the given player.
     *
     * @param denier the player denying the request
     */
    public void denyTeleportRequest(@NotNull Player denier) {
        if (!pendingRequests.containsKey(denier.getUniqueId())) {
            denier.sendMessage(plugin.getMessageManager().getMessage("tpa.no-pending"));
            return;
        }

        UUID requesterUUID = pendingRequests.remove(denier.getUniqueId());
        Player requester = plugin.getServer().getPlayer(requesterUUID);

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(plugin.getMessageManager().getMessage("tpa.denied", requester,
                    Map.of("target", denier.getName())));
        }
        denier.sendMessage(plugin.getMessageManager().getMessage("tpa.denied", denier,
                Map.of("target", requester != null ? requester.getName() : "Unknown")));
    }

    /**
     * Toggles whether a player accepts teleport requests.
     *
     * @param player the player to toggle TPA acceptance for
     */
    public void toggleTpa(@NotNull Player player) {
        if (tpaToggledOff.contains(player.getUniqueId())) {
            tpaToggledOff.remove(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage("tpa.toggle-on"));
        } else {
            tpaToggledOff.add(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage("tpa.toggle-off"));
        }
    }

    public void cleanup() {
        pendingRequests.clear();
        tpaToggledOff.clear();
    }
}
