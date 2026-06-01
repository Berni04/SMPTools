package com.smp.smptools.imagemap;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.Constants;
import com.smp.smptools.utils.BoundedInputStream;
import com.smp.smptools.utils.URLValidator;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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

                if (mapSection.isString(mapIdString)) {
                    String urlString = mapSection.getString(mapIdString);
                    loadImage(mapId, urlString, 0, 0, 1, 1);
                } else if (mapSection.isConfigurationSection(mapIdString)) {
                    ConfigurationSection section = mapSection.getConfigurationSection(mapIdString);
                    String urlString = section.getString("url");
                    int x = Math.max(0, section.getInt("x", 0));
                    int y = Math.max(0, section.getInt("y", 0));
                    int width = Math.max(1, Math.min(Constants.MAX_MAP_GRID_SIZE, section.getInt("width", 1)));
                    int height = Math.max(1, Math.min(Constants.MAX_MAP_GRID_SIZE, section.getInt("height", 1)));

                    if (width < 1 || height < 1) {
                        plugin.getLogger().warning("Invalid dimensions for map " + mapIdString + ", skipping");
                        continue;
                    }

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
                URL url = URLValidator.validateAndCreate(urlString);
                URLConnection conn = URLValidator.openConnection(url);

                int totalWidth = widthGrid * 128;
                int totalHeight = heightGrid * 128;

                BufferedImage fullImage;
                try (InputStream rawStream = conn.getInputStream();
                     InputStream boundedStream = new BoundedInputStream(rawStream, Constants.MAX_IMAGE_DOWNLOAD_BYTES)) {
                    fullImage = ImageProcessor.getImage(boundedStream, totalWidth, totalHeight);
                }

                if (fullImage != null) {
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
