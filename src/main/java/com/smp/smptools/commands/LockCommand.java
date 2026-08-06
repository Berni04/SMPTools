package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LockCommand implements CommandExecutor {

    private final SMPTools plugin;

    public LockCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    private OfflinePlayer resolveOfflinePlayer(String input) {
        if (input == null || input.isEmpty()) return null;
        try {
            UUID uuid = UUID.fromString(input);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            return Bukkit.getOfflinePlayerIfCached(input);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute lock commands.");
            return true;
        }

        if (!plugin.getConfig().getBoolean("features.container-locks.enabled", true)) {
            player.sendMessage("Container locking feature is currently disabled.");
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !plugin.getLockManager().isContainer(targetBlock)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You must be looking at a chest or container within 5 blocks!</red>"));
            return true;
        }

        String cmd = label.toLowerCase();
        if (cmd.equals("lock")) {
            if (plugin.getLockManager().isLocked(targetBlock)) {
                String owner = plugin.getLockManager().getOwnerName(targetBlock);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>This container is already locked by " + owner + ".</red>"));
                return true;
            }

            if (plugin.getLockManager().lockContainer(targetBlock, player)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>🔒 Container locked successfully!</green>"));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Could not lock this container.</red>"));
            }
            return true;
        }

        if (cmd.equals("unlock")) {
            if (!plugin.getLockManager().isLocked(targetBlock)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>This container is not locked.</red>"));
                return true;
            }

            if (plugin.getLockManager().unlockContainer(targetBlock, player)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>🔓 Container unlocked.</green>"));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You do not have permission to unlock this container.</red>"));
            }
            return true;
        }

        if (cmd.equals("trust")) {
            if (args.length == 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /trust <player></red>"));
                return true;
            }

            OfflinePlayer target = resolveOfflinePlayer(args[0]);
            if (target == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Could not find player '" + args[0] + "'. Provide a valid UUID or username of a player who has joined before.</red>"));
                return true;
            }

            if (plugin.getLockManager().trustPlayer(targetBlock, player, target)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Granted container access to " + (target.getName() != null ? target.getName() : args[0]) + ".</green>"));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Could not add trust. Make sure you own this locked container.</red>"));
            }
            return true;
        }

        if (cmd.equals("untrust")) {
            if (args.length == 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /untrust <player></red>"));
                return true;
            }

            OfflinePlayer target = resolveOfflinePlayer(args[0]);
            if (target == null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Could not find player '" + args[0] + "'. Provide a valid UUID or username of a player who has joined before.</red>"));
                return true;
            }

            if (plugin.getLockManager().untrustPlayer(targetBlock, player, target)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Removed container access for " + (target.getName() != null ? target.getName() : args[0]) + ".</yellow>"));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Could not remove trust.</red>"));
            }
            return true;
        }

        return true;
    }
}
