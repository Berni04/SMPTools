package com.smp.smptools.sleep;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SleepManager {

    private final SMPTools plugin;
    private boolean voteInProgress = false;
    private final Set<UUID> yesVotes = new HashSet<>();
    private final Set<UUID> noVotes = new HashSet<>();
    private Player voteInitiator;

    public SleepManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public boolean isVoteInProgress() {
        return voteInProgress;
    }

    public void startVote(Player player) {
        if (voteInProgress) {
            player.sendMessage(Component.text("A sleep vote is already in progress.", NamedTextColor.RED));
            return;
        }

        voteInProgress = true;
        voteInitiator = player;
        yesVotes.clear();
        noVotes.clear();
        
        // The initiator automatically votes yes
        yesVotes.add(player.getUniqueId());

        Component acceptButton = Component.text("[Accept]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/sleepvote accept"));
        Component denyButton = Component.text("[Deny]", NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/sleepvote deny"));

        Component formattedPlayerName = plugin.getChatManager().getFormattedDisplayName(player);
        Bukkit.broadcast(formattedPlayerName
                .append(Component.text(" wants to skip the night. ", NamedTextColor.YELLOW))
                .append(acceptButton)
                .append(Component.text(" "))
                .append(denyButton));
        
        checkVoteStatus();
    }

    public void addVote(Player player, boolean vote) {
        if (!voteInProgress) {
            player.sendMessage(Component.text("There is no sleep vote in progress.", NamedTextColor.RED));
            return;
        }

        UUID playerUUID = player.getUniqueId();
        if (yesVotes.contains(playerUUID) || noVotes.contains(playerUUID)) {
            player.sendMessage(Component.text("You have already voted.", NamedTextColor.RED));
            return;
        }

        Component formattedPlayerName = plugin.getChatManager().getFormattedDisplayName(player);
        if (vote) {
            yesVotes.add(playerUUID);
            Bukkit.broadcast(formattedPlayerName.append(Component.text(" has voted to skip the night.", NamedTextColor.GRAY)));
        } else {
            noVotes.add(playerUUID);
            Bukkit.broadcast(formattedPlayerName.append(Component.text(" has voted against skipping the night.", NamedTextColor.GRAY)));
        }

        checkVoteStatus();
    }

    private void checkVoteStatus() {
        int onlinePlayers = voteInitiator.getWorld().getPlayers().size();
        int requiredVotes = (int) Math.ceil(onlinePlayers / 2.0);

        if (yesVotes.size() >= requiredVotes) {
            Bukkit.broadcast(Component.text("The vote passed! Skipping the night.", NamedTextColor.GREEN));
            voteInitiator.getWorld().setTime(0);
            voteInitiator.getWorld().setThundering(false);
            voteInitiator.getWorld().setStorm(false);
            endVote();
        } else if (noVotes.size() >= (onlinePlayers - requiredVotes + 1)) {
            Bukkit.broadcast(Component.text("The vote failed. The night will not be skipped.", NamedTextColor.RED));
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
