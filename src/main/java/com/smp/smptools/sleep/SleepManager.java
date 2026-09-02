package com.smp.smptools.sleep;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SleepManager {

    public static class WorldVoteState {
        private final World world;
        private final UUID initiatorUuid;
        private final Set<UUID> yesVotes = ConcurrentHashMap.newKeySet();
        private final Set<UUID> noVotes = ConcurrentHashMap.newKeySet();

        public WorldVoteState(World world, UUID initiatorUuid) {
            this.world = world;
            this.initiatorUuid = initiatorUuid;
        }

        public World getWorld() { return world; }
        public UUID getInitiatorUuid() { return initiatorUuid; }
        public Set<UUID> getYesVotes() { return yesVotes; }
        public Set<UUID> getNoVotes() { return noVotes; }
    }

    private final SMPTools plugin;
    private final Map<UUID, WorldVoteState> activeVotes = new ConcurrentHashMap<>();

    public SleepManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public boolean isVoteInProgress() {
        return !activeVotes.isEmpty();
    }

    public boolean isVoteInProgress(World world) {
        return world != null && activeVotes.containsKey(world.getUID());
    }

    public void startVote(Player player) {
        World world = player.getWorld();
        if (isVoteInProgress(world)) {
            player.sendMessage(plugin.getMessageManager().getMessage("sleep.already-voting"));
            return;
        }
        if (world.getEnvironment() != World.Environment.NORMAL) {
            player.sendMessage(plugin.getMessageManager().getMessage("sleep.not-overworld"));
            return;
        }

        WorldVoteState state = new WorldVoteState(world, player.getUniqueId());
        activeVotes.put(world.getUID(), state);

        // The initiator automatically votes yes
        state.getYesVotes().add(player.getUniqueId());

        // AFK players automatically vote yes only if explicitly configured
        if (plugin.getAFKManager() != null && plugin.getConfig().getBoolean("features.afk.auto-vote-sleep", false)) {
            for (Player p : world.getPlayers()) {
                if (plugin.getAFKManager().isAFK(p)) {
                    state.getYesVotes().add(p.getUniqueId());
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
        
        checkVoteStatus(world);
    }

    public void addVote(Player player, boolean vote) {
        World world = player.getWorld();
        WorldVoteState state = activeVotes.get(world.getUID());
        if (state == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("sleep.no-vote"));
            return;
        }

        UUID playerUUID = player.getUniqueId();
        if (state.getYesVotes().contains(playerUUID) || state.getNoVotes().contains(playerUUID)) {
            player.sendMessage(plugin.getMessageManager().getMessage("sleep.already-voted"));
            return;
        }

        if (vote) {
            state.getYesVotes().add(playerUUID);
            world.sendMessage(plugin.getMessageManager().getMessage("sleep.voted-yes", player));
        } else {
            state.getNoVotes().add(playerUUID);
            world.sendMessage(plugin.getMessageManager().getMessage("sleep.voted-no", player));
        }

        checkVoteStatus(world);
    }

    private void checkVoteStatus(World world) {
        if (world == null) return;
        WorldVoteState state = activeVotes.get(world.getUID());
        if (state == null) return;

        int onlinePlayers = world.getPlayers().size();
        int requiredVotes = (int) Math.ceil(onlinePlayers / 2.0);

        if (state.getYesVotes().size() >= requiredVotes) {
            world.sendMessage(plugin.getMessageManager().getMessage("sleep.vote-accepted"));
            world.setTime(0);
            world.setThundering(false);
            world.setStorm(false);
            endVote(world);
        } else if (state.getNoVotes().size() >= (onlinePlayers - requiredVotes + 1)) {
            world.sendMessage(plugin.getMessageManager().getMessage("sleep.vote-failed"));
            endVote(world);
        }
    }

    public void endVote() {
        activeVotes.clear();
    }

    public void endVote(World world) {
        if (world != null) {
            activeVotes.remove(world.getUID());
        }
    }

    public Player getVoteInitiator() {
        if (activeVotes.isEmpty()) return null;
        WorldVoteState state = activeVotes.values().iterator().next();
        return org.bukkit.Bukkit.getPlayer(state.getInitiatorUuid());
    }

    public Player getVoteInitiator(World world) {
        if (world == null) return null;
        WorldVoteState state = activeVotes.get(world.getUID());
        return state != null ? org.bukkit.Bukkit.getPlayer(state.getInitiatorUuid()) : null;
    }
}
