package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Abstract base class for commands that are primarily intended for players
 * but may optionally allow console senders (e.g. admin info commands).
 *
 * <p>Provides common guard logic and delegates to {@link #onPlayerCommand(Player, Command, String, String[])}
 * for player senders. Subclasses that want to support console senders should override
 * {@link #allowConsole()} to return {@code true} and implement
 * {@link #onConsoleCommand(CommandSender, Command, String, String[])}.</p>
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
     * Returns whether this command allows non-player senders (e.g. console).
     * Override to return {@code true} to permit console use, then implement
     * {@link #onConsoleCommand(CommandSender, Command, String, String[])}.
     *
     * @return {@code true} if console senders are allowed
     */
    protected boolean allowConsole() {
        return false;
    }

    /**
     * Handles the command execution.
     * Routes to {@link #onPlayerCommand(Player, Command, String, String[])} for players,
     * or to {@link #onConsoleCommand(CommandSender, Command, String, String[])} for console
     * if {@link #allowConsole()} returns {@code true}.
     *
     * @param sender the command sender
     * @param command the command
     * @param label the command label
     * @param args the command arguments
     * @return true if the command was handled
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            return onPlayerCommand((Player) sender, command, label, args);
        }
        if (allowConsole()) {
            return onConsoleCommand(sender, command, label, args);
        }
        sender.sendMessage(plugin.getMessageManager().getMessage("common.player-only"));
        return true;
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

    /**
     * Called when a non-player (e.g. console) executes this command.
     * Only invoked if {@link #allowConsole()} returns {@code true}.
     * Default implementation sends {@code common.player-only} as a safety net.
     *
     * @param sender the command sender (console, command block, etc.)
     * @param command the command
     * @param label the command label
     * @param args the command arguments
     * @return true if the command was handled
     */
    protected boolean onConsoleCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(plugin.getMessageManager().getMessage("common.player-only"));
        return true;
    }
}
