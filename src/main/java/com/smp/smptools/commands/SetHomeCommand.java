package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.Constants;
import com.smp.smptools.utils.InputValidator;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;

public class SetHomeCommand extends AbstractPlayerCommand {

    public SetHomeCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                    java.util.Map.of("usage", "/sethome <name>")));
            return true;
        }

        String homeName = args[0].toLowerCase(Locale.ROOT);

        if (!InputValidator.isValidHomeName(homeName)) {
            player.sendMessage(plugin.getMessageManager().getMessage("homes.invalid-name", player,
                    java.util.Map.of("max", String.valueOf(Constants.MAX_HOME_NAME_LENGTH))));
            return true;
        }

        String playerUUID = player.getUniqueId().toString();

        int homeLimit = getHomeLimit(player);
        int currentHomes = 0;
        ConfigurationSection homesSection = plugin.getConfig().getConfigurationSection("homes." + playerUUID);
        if (homesSection != null) {
            currentHomes = homesSection.getKeys(false).size();
        }

        if (currentHomes >= homeLimit && !plugin.getConfig().contains("homes." + playerUUID + "." + homeName)) {
            player.sendMessage(plugin.getMessageManager().getMessage("homes.limit-reached", player,
                    java.util.Map.of("limit", String.valueOf(homeLimit))));
            return true;
        }

        Location location = player.getLocation();

        plugin.getConfig().set("homes." + playerUUID + "." + homeName + ".world", location.getWorld().getName());
        plugin.getConfig().set("homes." + playerUUID + "." + homeName + ".x", location.getX());
        plugin.getConfig().set("homes." + playerUUID + "." + homeName + ".y", location.getY());
        plugin.getConfig().set("homes." + playerUUID + "." + homeName + ".z", location.getZ());
        plugin.getConfig().set("homes." + playerUUID + "." + homeName + ".yaw", location.getYaw());
        plugin.getConfig().set("homes." + playerUUID + "." + homeName + ".pitch", location.getPitch());
        plugin.saveConfig();

        player.sendMessage(plugin.getMessageManager().getMessage("homes.set", player,
                java.util.Map.of("name", homeName)));
        return true;
    }

    private int getHomeLimit(Player player) {
        ConfigurationSection limitsSection = plugin.getConfig().getConfigurationSection("home-limits");
        if (limitsSection == null) {
            return 1;
        }

        int maxLimit = 0;
        for (String group : limitsSection.getKeys(false)) {
            if (player.hasPermission("smptools.homes." + group)) {
                int limit = limitsSection.getInt(group);
                if (limit > maxLimit) {
                    maxLimit = limit;
                }
            }
        }
        return maxLimit > 0 ? maxLimit : limitsSection.getInt("default", 1);
    }
}
