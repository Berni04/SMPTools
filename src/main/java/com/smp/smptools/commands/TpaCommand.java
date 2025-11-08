package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCommand implements CommandExecutor {

    private final SMPTools plugin;

    public TpaCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (label.equalsIgnoreCase("tpr")) {
            if (args.length == 0) {
                player.sendMessage(Component.text("Usage: /tpr <player>", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(Component.text("Player not found or is offline.", NamedTextColor.RED));
                return true;
            }
            if (player.equals(target)) {
                player.sendMessage(Component.text("You cannot send a teleport request to yourself.", NamedTextColor.RED));
                return true;
            }
            plugin.getTpaManager().sendTeleportRequest(player, target);
            return true;
        } else if (label.equalsIgnoreCase("tpa")) {
            plugin.getTpaManager().acceptTeleportRequest(player);
            return true;
        } else if (label.equalsIgnoreCase("tpd")) {
            plugin.getTpaManager().denyTeleportRequest(player);
            return true;
        } else if (label.equalsIgnoreCase("tptoggle")) {
            plugin.getTpaManager().toggleTpa(player);
            return true;
        }

        return false;
    }
}
