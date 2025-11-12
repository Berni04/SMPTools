package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand implements CommandExecutor {

    private final SMPTools plugin;

    public HomeCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Please specify a home name. Usage: /home <name>");
            return true;
        }

        Player player = (Player) sender;
        String playerUUID = player.getUniqueId().toString();
        String homeName = args[0].toLowerCase();

        if (plugin.getConfig().contains("homes." + playerUUID + "." + homeName)) {
            String worldName = plugin.getConfig().getString("homes." + playerUUID + "." + homeName + ".world");
            double x = plugin.getConfig().getDouble("homes." + playerUUID + "." + homeName + ".x");
            double y = plugin.getConfig().getDouble("homes." + playerUUID + "." + homeName + ".y");
            double z = plugin.getConfig().getDouble("homes." + playerUUID + "." + homeName + ".z");
            float yaw = (float) plugin.getConfig().getDouble("homes." + playerUUID + "." + homeName + ".yaw");
            float pitch = (float) plugin.getConfig().getDouble("homes." + playerUUID + "." + homeName + ".pitch");

            Location homeLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
            plugin.getTeleportManager().startTeleport(player, homeLocation, "'" + homeName + "'");
        } else {
            player.sendMessage(ChatColor.RED + "You don't have a home named '" + homeName + "'.");
        }
        return true;
    }
}
