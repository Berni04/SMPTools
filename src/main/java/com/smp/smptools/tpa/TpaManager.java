package com.smp.smptools.tpa;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TpaManager {

    private final SMPTools plugin;
    private final Map<UUID, UUID> pendingRequests = new HashMap<>(); // Target UUID -> Requester UUID
    private final Set<UUID> tpaToggledOff = new HashSet<>(); // Players who have /tptoggle off

    public TpaManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public void sendTeleportRequest(Player requester, Player target) {
        if (tpaToggledOff.contains(target.getUniqueId())) {
            requester.sendMessage(Component.text(target.getName() + " is not accepting teleport requests at this time.", NamedTextColor.RED));
            return;
        }

        if (pendingRequests.containsKey(target.getUniqueId())) {
            requester.sendMessage(Component.text(target.getName() + " already has a pending teleport request.", NamedTextColor.RED));
            return;
        }

        pendingRequests.put(target.getUniqueId(), requester.getUniqueId());

        requester.sendMessage(Component.text("Teleport request sent to " + target.getName() + ".", NamedTextColor.GREEN));

        Component acceptButton = Component.text("[Accept]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/tpa"));
        Component denyButton = Component.text("[Deny]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/tpd"));

        target.sendMessage(Component.text(requester.getName() + " has requested to teleport to you. ", NamedTextColor.YELLOW)
                .append(acceptButton)
                .append(Component.text(" "))
                .append(denyButton));

        // Timeout the request after 60 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRequests.containsKey(target.getUniqueId()) && pendingRequests.get(target.getUniqueId()).equals(requester.getUniqueId())) {
                    pendingRequests.remove(target.getUniqueId());
                    requester.sendMessage(Component.text("Your teleport request to " + target.getName() + " has expired.", NamedTextColor.RED));
                    target.sendMessage(Component.text("Teleport request from " + requester.getName() + " has expired.", NamedTextColor.RED));
                }
            }
        }.runTaskLater(plugin, 20 * 60); // 60 seconds
    }

    public void acceptTeleportRequest(Player acceptor) {
        if (!pendingRequests.containsKey(acceptor.getUniqueId())) {
            acceptor.sendMessage(Component.text("You have no pending teleport requests.", NamedTextColor.RED));
            return;
        }

        UUID requesterUUID = pendingRequests.remove(acceptor.getUniqueId());
        Player requester = plugin.getServer().getPlayer(requesterUUID);

        if (requester == null || !requester.isOnline()) {
            acceptor.sendMessage(Component.text("The requester is no longer online.", NamedTextColor.RED));
            return;
        }

        requester.teleport(acceptor.getLocation());
        requester.sendMessage(Component.text("Teleport successful!", NamedTextColor.GREEN));
        acceptor.sendMessage(Component.text(requester.getName() + " has teleported to you.", NamedTextColor.GREEN));
    }

    public void denyTeleportRequest(Player denier) {
        if (!pendingRequests.containsKey(denier.getUniqueId())) {
            denier.sendMessage(Component.text("You have no pending teleport requests.", NamedTextColor.RED));
            return;
        }

        UUID requesterUUID = pendingRequests.remove(denier.getUniqueId());
        Player requester = plugin.getServer().getPlayer(requesterUUID);

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(Component.text(denier.getName() + " has denied your teleport request.", NamedTextColor.RED));
        }
        denier.sendMessage(Component.text("Teleport request denied.", NamedTextColor.GREEN));
    }

    public void toggleTpa(Player player) {
        if (tpaToggledOff.contains(player.getUniqueId())) {
            tpaToggledOff.remove(player.getUniqueId());
            player.sendMessage(Component.text("You are now accepting teleport requests.", NamedTextColor.GREEN));
        } else {
            tpaToggledOff.add(player.getUniqueId());
            player.sendMessage(Component.text("You are no longer accepting teleport requests.", NamedTextColor.RED));
        }
    }
}
