package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.Constants;
import com.smp.smptools.utils.InputValidator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;

public class SetHomeCommand implements CommandExecutor {

    private final SMPTools plugin;

    public SetHomeCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Please specify a name for your home. Usage: /sethome <name>", NamedTextColor.RED));
            return true;
        }

        String homeName = args[0].toLowerCase(Locale.ROOT);

        if (!InputValidator.isValidHomeName(homeName)) {
            sender.sendMessage(Component.text("Invalid home name. Use only letters, numbers, _ and - (max " + Constants.MAX_HOME_NAME_LENGTH + " characters).", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        String playerUUID = player.getUniqueId().toString();

        int homeLimit = getHomeLimit(player);
        int currentHomes = 0;
        ConfigurationSection homesSection = plugin.getConfig().getConfigurationSection("homes." + playerUUID);
        if (homesSection != null) {
            currentHomes = homesSection.getKeys(false).size();
        }

        if (currentHomes >= homeLimit && !plugin.getConfig().contains("homes." + playerUUID + "." + homeName)) {
            player.sendMessage(Component.text("You have reached your home limit of " + homeLimit + " homes.", NamedTextColor.RED));
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

        player.sendMessage(Component.text("Your home '" + homeName + "' has been set!", NamedTextColor.GREEN));
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
