package com.smp.smptools.sleep;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SleepManager {

    private final SMPTools plugin;
    private boolean voteInProgress = false;
    private final Set<UUID> yesVotes = ConcurrentHashMap.newKeySet();
    private final Set<UUID> noVotes = ConcurrentHashMap.newKeySet();
    private Player voteInitiator;

    public SleepManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public boolean isVoteInProgress() {
        return voteInProgress;
    }

    public void startVote(Player player) {
        if (voteInProgress) {
            player.sendMessage(plugin.getMessageManager().getMessage("sleep.already-voting"));
            return;
        }

        voteInProgress = true;
        voteInitiator = player;
        yesVotes.clear();
        noVotes.clear();
        
        // The initiator automatically votes yes
        yesVotes.add(player.getUniqueId());

        // AFK players automatically vote yes
        if (plugin.getAFKManager() != null && plugin.getConfig().getBoolean("features.afk.auto-vote-sleep", true)) {
            for (Player p : player.getWorld().getPlayers()) {
                if (plugin.getAFKManager().isAFK(p)) {
                    yesVotes.add(p.getUniqueId());
                }
            }
        }

        Component acceptButton = Component.text("[Accept]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/sleepvote accept"));
        Component denyButton = Component.text("[Deny]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/sleepvote deny"));

        Component voteMessage = plugin.getMessageManager().getMessage("sleep.vote-started", player);
        Bukkit.broadcast(voteMessage
                .append(Component.text(" "))
                .append(acceptButton)
                .append(Component.text(" "))
                .append(denyButton));
        
        checkVoteStatus();
    }

    public void addVote(Player player, boolean vote) {
        if (!voteInProgress) {
            player.sendMessage(plugin.getMessageManager().getMessage("sleep.no-vote"));
            return;
        }

        UUID playerUUID = player.getUniqueId();
        if (yesVotes.contains(playerUUID) || noVotes.contains(playerUUID)) {
            player.sendMessage(plugin.getMessageManager().getMessage("sleep.already-voted"));
            return;
        }

        if (vote) {
            yesVotes.add(playerUUID);
            Bukkit.broadcast(plugin.getMessageManager().getMessage("sleep.voted-yes", player));
        } else {
            noVotes.add(playerUUID);
            Bukkit.broadcast(plugin.getMessageManager().getMessage("sleep.voted-no", player));
        }

        checkVoteStatus();
    }

    private void checkVoteStatus() {
        int onlinePlayers = voteInitiator.getWorld().getPlayers().size();
        int requiredVotes = (int) Math.ceil(onlinePlayers / 2.0);

        if (yesVotes.size() >= requiredVotes) {
            Bukkit.broadcast(plugin.getMessageManager().getMessage("sleep.vote-accepted"));
            voteInitiator.getWorld().setTime(0);
            voteInitiator.getWorld().setThundering(false);
            voteInitiator.getWorld().setStorm(false);
            endVote();
        } else if (noVotes.size() >= (onlinePlayers - requiredVotes + 1)) {
            Bukkit.broadcast(plugin.getMessageManager().getMessage("sleep.vote-failed"));
            endVote();
        }
    }

    public void endVote() {
        voteInProgress = false;
        yesVotes.clear();
        noVotes.clear();
        voteInitiator = null;
    }

    public Player getVoteInitiator() {
        return voteInitiator;
    }
}
