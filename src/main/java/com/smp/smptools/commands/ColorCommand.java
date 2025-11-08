package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ColorCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        SMPTools plugin = SMPTools.getInstance();

        if (args.length == 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /color <<color>|clear></red>"));
            return true;
        }

        String colorInput = String.join(" ", args);

        if (colorInput.equalsIgnoreCase("clear")) {
            plugin.getStatsConfig().set("players." + player.getUniqueId() + ".name-color", null);
            plugin.saveStatsConfig();
            plugin.getNameTagListener().updatePlayerName(player);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Your name color has been cleared.</green>"));
            return true;
        }

        // For a color, we should probably only allow a single color tag, but for now we'll allow any format
        plugin.getStatsConfig().set("players." + player.getUniqueId() + ".name-color", colorInput);
        plugin.saveStatsConfig();
        plugin.getNameTagListener().updatePlayerName(player);

        Component colorPreview = MiniMessage.miniMessage().deserialize(colorInput + player.getName());
        player.sendMessage(Component.text("Your name color has been set to: ").append(colorPreview));

        return true;
    }
}
