package com.smp.smptools.sleep;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
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

        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            player.sendMessage(plugin.getMessageManager().getMessage("sleep.not-overworld"));
            return;
        }

        voteInProgress = true;
        voteInitiator = player;
        yesVotes.clear();
        noVotes.clear();
        
        // The initiator automatically votes yes
        yesVotes.add(player.getUniqueId());

        // AFK players automatically vote yes only if explicitly configured
        if (plugin.getAFKManager() != null && plugin.getConfig().getBoolean("features.afk.auto-vote-sleep", false)) {
            for (Player p : world.getPlayers()) {
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
        Component broadcastMsg = voteMessage
                .append(Component.text(" "))
                .append(acceptButton)
                .append(Component.text(" "))
                .append(denyButton);

        world.sendMessage(broadcastMsg);
        
        checkVoteStatus();
    }

    public void addVote(Player player, boolean vote) {
        if (!voteInProgress) {
            player.sendMessage(plugin.getMessageManager().getMessage("sleep.no-vote"));
            return;
        }

        if (voteInitiator == null || !player.getWorld().equals(voteInitiator.getWorld())) {
            player.sendMessage(plugin.getMessageManager().getMessage("sleep.wrong-world"));
            return;
        }

        UUID playerUUID = player.getUniqueId();
        if (yesVotes.contains(playerUUID) || noVotes.contains(playerUUID)) {
            player.sendMessage(plugin.getMessageManager().getMessage("sleep.already-voted"));
            return;
        }

        World world = player.getWorld();
        if (vote) {
            yesVotes.add(playerUUID);
            world.sendMessage(plugin.getMessageManager().getMessage("sleep.voted-yes", player));
        } else {
            noVotes.add(playerUUID);
            world.sendMessage(plugin.getMessageManager().getMessage("sleep.voted-no", player));
        }

        checkVoteStatus();
    }

    private void checkVoteStatus() {
        if (voteInitiator == null || !voteInitiator.isOnline() || voteInitiator.getWorld() == null) {
            endVote();
            return;
        }

        World world = voteInitiator.getWorld();
        int onlinePlayers = world.getPlayers().size();
        int requiredVotes = (int) Math.ceil(onlinePlayers / 2.0);

        if (yesVotes.size() >= requiredVotes) {
            world.sendMessage(plugin.getMessageManager().getMessage("sleep.vote-accepted"));
            world.setTime(0);
            world.setThundering(false);
            world.setStorm(false);
            endVote();
        } else if (noVotes.size() >= (onlinePlayers - requiredVotes + 1)) {
            world.sendMessage(plugin.getMessageManager().getMessage("sleep.vote-failed"));
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
