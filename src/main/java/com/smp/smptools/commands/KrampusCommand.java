package com.smp.smptools.commands;

import com.smp.smptools.christmas.KrampusManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KrampusCommand implements CommandExecutor {

    private final KrampusManager krampusManager;

    public KrampusCommand(KrampusManager krampusManager) {
        this.krampusManager = krampusManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("smptools.admin")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("spawn")) {
            krampusManager.spawnKrampus(player.getLocation());
            player.sendMessage(Component.text("Krampus has been summoned!", NamedTextColor.DARK_RED));
        } else {
            player.sendMessage(Component.text("Usage: /krampus spawn", NamedTextColor.RED));
        }

        return true;
    }
}
