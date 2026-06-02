package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Abstract base class for commands that can only be used by players.
 * Provides common guard logic for player-only commands,
 * reducing boilerplate code in implementing classes.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public abstract class AbstractPlayerCommand implements CommandExecutor {

    protected final SMPTools plugin;

    /**
     * Constructs an AbstractPlayerCommand with plugin reference.
     *
     * @param plugin the SMPTools plugin instance
     */
    public AbstractPlayerCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the command execution.
     * Checks if the sender is a player and delegates to onPlayerCommand().
     *
     * @param sender the command sender
     * @param command the command
     * @param label the command label
     * @param args the command arguments
     * @return true if the command was handled
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("common.player-only"));
            return true;
        }
        return onPlayerCommand((Player) sender, command, label, args);
    }

    /**
     * Called when a player executes this command.
     * Implementations should handle the command logic here.
     *
     * @param player the player who executed the command
     * @param command the command
     * @param label the command label
     * @param args the command arguments
     * @return true if the command was handled
     */
    protected abstract boolean onPlayerCommand(Player player, Command command, String label, String[] args);
}
