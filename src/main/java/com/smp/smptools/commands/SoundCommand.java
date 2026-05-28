package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /sound <sound_name>", NamedTextColor.RED));
            // Optionally, list available sounds
            ConfigurationSection soundsSection = plugin.getConfig().getConfigurationSection("features.meme-sounds.sounds");
            if (soundsSection != null) {
                sender.sendMessage(Component.text("Available sounds: " + String.join(", ", soundsSection.getKeys(false)), NamedTextColor.YELLOW));
            }
            return true;
        }

        Player player = (Player) sender;
        String soundName = args[0].toLowerCase();
        String soundKey = plugin.getConfig().getString("features.meme-sounds.sounds." + soundName);

        if (soundKey == null) {
            player.sendMessage(Component.text("That sound does not exist.", NamedTextColor.RED));
            return true;
        }

        // Play the sound for everyone in the world
        for (Player p : player.getWorld().getPlayers()) {
            p.playSound(player.getLocation(), soundKey, SoundCategory.MASTER, 1.0f, 1.0f);
        }

        return true;
    }
}
