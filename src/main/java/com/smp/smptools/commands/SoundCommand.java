package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.ChatColor;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SoundCommand implements CommandExecutor {

    private final SMPTools plugin;

    public SoundCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /sound <sound_name>");
            // Optionally, list available sounds
            ConfigurationSection soundsSection = plugin.getConfig().getConfigurationSection("features.meme-sounds.sounds");
            if (soundsSection != null) {
                sender.sendMessage(ChatColor.YELLOW + "Available sounds: " + String.join(", ", soundsSection.getKeys(false)));
            }
            return true;
        }

        Player player = (Player) sender;
        String soundName = args[0].toLowerCase();
        String soundKey = plugin.getConfig().getString("features.meme-sounds.sounds." + soundName);

        if (soundKey == null) {
            player.sendMessage(ChatColor.RED + "That sound does not exist.");
            return true;
        }

        // Play the sound for everyone in the world
        for (Player p : player.getWorld().getPlayers()) {
            p.playSound(player.getLocation(), soundKey, SoundCategory.MASTER, 1.0f, 1.0f);
        }

        return true;
    }
}
