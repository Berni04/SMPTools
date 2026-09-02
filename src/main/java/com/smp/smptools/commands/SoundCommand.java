package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SoundCommand extends AbstractPlayerCommand {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 5000L;

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
                        Map.of("sounds", String.join(", ", soundsSection.getKeys(false)))));
            }
            return true;
        }

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS && !player.hasPermission("smptools.sound.bypass")) {
            long remaining = (COOLDOWN_MS - (now - last)) / 1000L + 1;
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Please wait " + remaining + "s before playing another sound.</red>"));
            return true;
        }

        String soundName = args[0].toLowerCase();
        String soundKey = plugin.getConfig().getString("features.meme-sounds.sounds." + soundName);

        if (soundKey == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("sound.not-found"));
            return true;
        }

        cooldowns.put(player.getUniqueId(), now);

        for (Player p : player.getWorld().getPlayers()) {
            p.playSound(player.getLocation(), soundKey, SoundCategory.MASTER, 1.0f, 1.0f);
        }

        return true;
    }
}
