package com.smp.smptools.imagemap;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.net.URL;

public class MapManager {

    private final SMPTools plugin;

    public MapManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public void loadMaps() {
        ConfigurationSection mapSection = plugin.getImageMapsConfig().getConfigurationSection("maps");
        if (mapSection == null) {
            return;
        }

        for (String mapIdString : mapSection.getKeys(false)) {
            try {
                int mapId = Integer.parseInt(mapIdString);
                String urlString = mapSection.getString(mapIdString);

                new Thread(() -> {
                    try {
                        URL url = new URL(urlString);
                        BufferedImage image = ImageProcessor.getImage(url);

                        if (image != null) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                MapView mapView = Bukkit.getMap(mapId);
                                if (mapView != null) {
                                    mapView.getRenderers().forEach(mapView::removeRenderer);
                                    mapView.addRenderer(new ImageMapRenderer(image));
                                    plugin.getLogger().info("Reloaded custom map ID: " + mapId);
                                }
                            });
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to reload map " + mapId + " from URL " + urlString + ": " + e.getMessage());
                    }
                }).start();

            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid map ID in imagemaps.yml: " + mapIdString);
            }
        }
    }
}
