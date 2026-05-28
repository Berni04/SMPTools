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

                // Check if it's the old format (String) or new format (ConfigurationSection)
                if (mapSection.isString(mapIdString)) {
                    // Legacy Format
                    String urlString = mapSection.getString(mapIdString);
                    loadImage(mapId, urlString, 0, 0, 1, 1);
                } else if (mapSection.isConfigurationSection(mapIdString)) {
                    // New Format
                    ConfigurationSection section = mapSection.getConfigurationSection(mapIdString);
                    String urlString = section.getString("url");
                    int x = section.getInt("x", 0);
                    int y = section.getInt("y", 0);
                    int width = section.getInt("width", 1);
                    int height = section.getInt("height", 1);
                    loadImage(mapId, urlString, x, y, width, height);
                }

            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid map ID in imagemaps.yml: " + mapIdString);
            }
        }
    }

    private void loadImage(int mapId, String urlString, int xGrid, int yGrid, int widthGrid, int heightGrid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(urlString);
                // Calculate total size based on grid
                int totalWidth = widthGrid * 128;
                int totalHeight = heightGrid * 128;

                BufferedImage fullImage = ImageProcessor.getImage(url, totalWidth, totalHeight);

                if (fullImage != null) {
                    // Extract the specific sub-image for this map
                    BufferedImage subImage = ImageProcessor.getSubImage(fullImage, xGrid * 128, yGrid * 128, 128, 128);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        MapView mapView = Bukkit.getMap(mapId);
                        if (mapView != null) {
                            mapView.getRenderers().forEach(mapView::removeRenderer);
                            mapView.addRenderer(new ImageMapRenderer(subImage));
                            plugin.getLogger().info("Reloaded custom map ID: " + mapId);
                        }
                    });
                }
            } catch (Exception e) {
                plugin.getLogger()
                        .warning("Failed to reload map " + mapId + " from URL " + urlString + ": " + e.getMessage());
            }
        });
    }
}
