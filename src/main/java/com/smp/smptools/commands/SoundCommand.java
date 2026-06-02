package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SoundCommand extends AbstractPlayerCommand {

    public SoundCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("sound.usage"));
            ConfigurationSection soundsSection = plugin.getConfig().getConfigurationSection("features.meme-sounds.sounds");
            if (soundsSection != null) {
                player.sendMessage(plugin.getMessageManager().getMessage("sound.available", player,
                        java.util.Map.of("sounds", String.join(", ", soundsSection.getKeys(false)))));
            }
            return true;
        }

        String soundName = args[0].toLowerCase();
        String soundKey = plugin.getConfig().getString("features.meme-sounds.sounds." + soundName);

        if (soundKey == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("sound.not-found"));
            return true;
        }

        for (Player p : player.getWorld().getPlayers()) {
            p.playSound(player.getLocation(), soundKey, SoundCategory.MASTER, 1.0f, 1.0f);
        }

        return true;
    }
}
