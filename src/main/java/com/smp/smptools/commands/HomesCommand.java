package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Set;

public class HomesCommand implements CommandExecutor {

    private final SMPTools plugin;

    public HomesCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        String playerUUID = player.getUniqueId().toString();

        ConfigurationSection homesSection = plugin.getConfig().getConfigurationSection("homes." + playerUUID);
        if (homesSection == null) {
            player.sendMessage(ChatColor.RED + "You have no homes set.");
            return true;
        }

        Set<String> homeNames = homesSection.getKeys(false);
        if (homeNames.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You have no homes set.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "Your homes: " + String.join(", ", homeNames));
        return true;
    }
}
