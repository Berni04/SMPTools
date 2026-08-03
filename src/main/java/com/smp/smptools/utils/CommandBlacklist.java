package com.smp.smptools.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Centralized blacklist of dangerous Bukkit/Paper commands that should never
 * be executed from configuration-driven reward flows.
 *
 * <p>Reward entries like {@code command:op Player} or {@code command:stop} are
 * almost always misconfigurations or malicious YAML edits. Rejecting them
 * early prevents privilege escalation when the reward flow uses
 * {@code Bukkit.dispatchCommand(console, ...)}.</p>
 */
public final class CommandBlacklist {

    private static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
            "op", "deop", "stop", "reload", "rl", "restart", "whitelist",
            "plugins", "pl", "version", "ver", "?", "help",
            "save-all", "save-off", "save-on", "reload-command",
            "bukkit:help", "minecraft:stop", "minecraft:reload"
    ));

    private CommandBlacklist() {
    }

    /**
     * Returns true if the given command string starts with a blocked command.
     * The check is case-insensitive and ignores the leading slash.
     *
     * @param command the full command string (without leading slash)
     * @return true if the command should be rejected
     */
    public static boolean isBlocked(String command) {
        if (command == null || command.isEmpty()) return true;
        String trimmed = command.trim().replaceFirst("^/+", "");
        int space = trimmed.indexOf(' ');
        String head = (space == -1 ? trimmed : trimmed.substring(0, space)).toLowerCase();
        if (head.contains(":")) {
            head = head.substring(head.indexOf(':') + 1);
        }
        return BLOCKED.contains(head);
    }
}
