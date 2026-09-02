package com.smp.smptools.imagemap;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.AbstractPlayerCommand;
import com.smp.smptools.utils.Constants;
import com.smp.smptools.utils.BoundedInputStream;
import com.smp.smptools.utils.URLValidator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

public class MapCommand extends AbstractPlayerCommand {

    public MapCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("map.usage"));
            return true;
        }

        String urlString = args[0];

        // Validate URL
        URL url;
        try {
            url = URLValidator.validateAndCreate(urlString);
        } catch (Exception e) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.invalid-url", player, Map.of("error", e.getMessage())));
            return true;
        }

        int widthGrid = 1;
        int heightGrid = 1;

        if (args.length >= 3) {
            try {
                widthGrid = Integer.parseInt(args[1]);
                heightGrid = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessageManager().getMessage("map.not-numbers", player));
                return true;
            }

            // Validate grid bounds
            if (widthGrid < Constants.MIN_MAP_GRID_SIZE || widthGrid > Constants.MAX_MAP_GRID_SIZE ||
                heightGrid < Constants.MIN_MAP_GRID_SIZE || heightGrid > Constants.MAX_MAP_GRID_SIZE) {
                player.sendMessage(plugin.getMessageManager().getMessage("map.invalid-dimensions", player,
                        Map.of("min", String.valueOf(Constants.MIN_MAP_GRID_SIZE), "max", String.valueOf(Constants.MAX_MAP_GRID_SIZE))));
                return true;
            }
        }

        player.sendMessage(plugin.getMessageManager().getMessage("map.downloading", player));

        int finalWidthGrid = widthGrid;
        int finalHeightGrid = heightGrid;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URLConnection conn = URLValidator.openConnection(url);
                int totalWidth = finalWidthGrid * 128;
                int totalHeight = finalHeightGrid * 128;

                BufferedImage fullImage;
                try (BoundedInputStream bis = new BoundedInputStream(conn.getInputStream(), Constants.MAX_IMAGE_DOWNLOAD_BYTES);
                     java.io.InputStream is = bis) {
                    fullImage = ImageProcessor.getImage(is, totalWidth, totalHeight);
                }

                if (fullImage == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(plugin.getMessageManager().getMessage("map.download-failed", player));
                    });
                    return;
                }

                // Process and render the image on the main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (int x = 0; x < finalWidthGrid; x++) {
                        for (int y = 0; y < finalHeightGrid; y++) {

                            BufferedImage subImage = ImageProcessor.getSubImage(fullImage, x * 128, y * 128, 128, 128);

                            MapView mapView = Bukkit.createMap(player.getWorld());
                            mapView.getRenderers().forEach(mapView::removeRenderer);
                            mapView.addRenderer(new ImageMapRenderer(subImage));

                            ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                            MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
                            mapMeta.setMapView(mapView);
                            mapItem.setItemMeta(mapMeta);

                            // Save for persistence
                            String path = "maps." + mapView.getId();
                            plugin.getImageMapsConfig().set(path + ".url", urlString);
                            plugin.getImageMapsConfig().set(path + ".x", x);
                            plugin.getImageMapsConfig().set(path + ".y", y);
                            plugin.getImageMapsConfig().set(path + ".width", finalWidthGrid);
                            plugin.getImageMapsConfig().set(path + ".height", finalHeightGrid);

                            player.getInventory().addItem(mapItem);
                        }
                    }
                    plugin.saveImageMapsConfig();
                    player.sendMessage(plugin.getMessageManager().getMessage("map.created", player));
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(plugin.getMessageManager().getMessage("map.download-failed", player));
                });
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to create map for " + player.getName() + " from " + urlString + ": " + e.getMessage(), e);
            }
        });

        return true;
    }
}
