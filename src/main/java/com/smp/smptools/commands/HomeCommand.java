package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.Constants;
import com.smp.smptools.utils.InputValidator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.Locale;

public class HomeCommand extends AbstractPlayerCommand {

    public HomeCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (plugin.getKrampusManager() != null && plugin.getKrampusManager().isKidnapped(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("homes.caged", player));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                    java.util.Map.of("usage", "/home <name>")));
            return true;
        }

        String homeName = args[0].toLowerCase(Locale.ROOT);

        if (!InputValidator.isValidHomeName(homeName)) {
            player.sendMessage(plugin.getMessageManager().getMessage("homes.invalid-name", player,
                    java.util.Map.of("max", String.valueOf(Constants.MAX_HOME_NAME_LENGTH))));
            return true;
        }

        String playerUUID = player.getUniqueId().toString();

        if (plugin.getConfig().contains("homes." + playerUUID + "." + homeName)) {
            String worldName = plugin.getConfig().getString("homes." + playerUUID + "." + homeName + ".world");
            double x = plugin.getConfig().getDouble("homes." + playerUUID + "." + homeName + ".x");
            double y = plugin.getConfig().getDouble("homes." + playerUUID + "." + homeName + ".y");
            double z = plugin.getConfig().getDouble("homes." + playerUUID + "." + homeName + ".z");
            float yaw = (float) plugin.getConfig().getDouble("homes." + playerUUID + "." + homeName + ".yaw");
            float pitch = (float) plugin.getConfig().getDouble("homes." + playerUUID + "." + homeName + ".pitch");

            if (worldName == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("homes.world-not-found"));
                return true;
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("homes.world-not-found"));
                return true;
            }

            Location homeLocation = new Location(world, x, y, z, yaw, pitch);
            plugin.getTeleportManager().startTeleport(player, homeLocation, "'" + homeName + "'");
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("homes.not-found", player,
                    java.util.Map.of("name", homeName)));
        }
        return true;
    }
}
