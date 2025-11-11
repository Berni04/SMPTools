package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ResourcePackListener implements Listener {

    private final SMPTools plugin;

    public ResourcePackListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String resourcePackUrl = plugin.getConfig().getString("features.meme-sounds.resource-pack-url");

        if (resourcePackUrl != null && !resourcePackUrl.isEmpty() && !resourcePackUrl.equals("YOUR_RESOURCE_PACK_URL_HERE")) {
            // A small delay can help ensure the packet is sent reliably
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.setResourcePack(resourcePackUrl);
            }, 20L); // 1 second delay
        }
    }
}
