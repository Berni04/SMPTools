package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InvseeCommand implements CommandExecutor {

    private final SMPTools plugin;

    public InvseeCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can use this command!</red>"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("smptools.invsee")) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to use this command.</red>"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /invsee <player></red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found or is offline.</red>"));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot invsee yourself. Use /inv to open your own inventory.</red>"));
            return true;
        }

        player.openInventory(target.getInventory());
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Viewing " + target.getName() + "'s inventory.</green>"));

        return true;
    }
}
