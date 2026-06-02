package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.InputValidator;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.regex.Pattern;

public class ColorCommand extends AbstractPlayerCommand {

    /**
     * Allowlist of characters permitted in user-supplied color strings. Limits
     * input to MiniMessage color/formatting tags and standard Minecraft color
     * codes (e.g. {@code &a}, {@code &#FF00FF}). Anything else is rejected
     * because {@link MiniMessage#deserialize(String)} is lenient and would not
     * throw for arbitrary user input.
     */
    private static final Pattern COLOR_ALLOWLIST = Pattern.compile("^[a-zA-Z0-9_#!<>\\-/: ]+$");

    public ColorCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player, Map.of("usage", "/color <color|clear>")));
            return true;
        }

        String colorInput = String.join(" ", args);

        if (colorInput.equalsIgnoreCase("clear")) {
            plugin.getStatsConfig().set("players." + player.getUniqueId() + ".name-color", null);
            plugin.saveStatsConfig();
            plugin.getNameTagListener().updatePlayerName(player);
            player.sendMessage(plugin.getMessageManager().getMessage("color.cleared"));
            return true;
        }

        String sanitizedColor = InputValidator.sanitizeMiniMessage(colorInput);

        // Reject any input that contains characters outside the allowlist,
        // since MiniMessage is lenient and silently accepts most invalid
        // structures without throwing.
        if (!COLOR_ALLOWLIST.matcher(sanitizedColor).matches()) {
            player.sendMessage(plugin.getMessageManager().getMessage("color.invalid", player));
            return true;
        }

        try {
            MiniMessage.miniMessage().deserialize(sanitizedColor);
        } catch (Exception e) {
            player.sendMessage(plugin.getMessageManager().getMessage("color.invalid", player));
            return true;
        }

        plugin.getStatsConfig().set("players." + player.getUniqueId() + ".name-color", sanitizedColor);
        plugin.saveStatsConfig();
        plugin.getNameTagListener().updatePlayerName(player);

        player.sendMessage(plugin.getMessageManager().getMessage("color.set", player, Map.of("color", sanitizedColor)));

        return true;
    }
}
