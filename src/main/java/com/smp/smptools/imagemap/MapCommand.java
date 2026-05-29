package com.smp.smptools.imagemap;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.net.URL;

public class MapCommand implements CommandExecutor {

    private final SMPTools plugin;

    public MapCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /tomap <url> [width] [height]", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        String urlString = args[0];

        int widthGrid = 1;
        int heightGrid = 1;

        if (args.length >= 3) {
            try {
                widthGrid = Integer.parseInt(args[1]);
                heightGrid = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Width and Height must be numbers.", NamedTextColor.RED));
                return true;
            }
        }

        player.sendMessage(Component.text("Downloading and processing image... this may take a moment.", NamedTextColor.GRAY));

        int finalWidthGrid = widthGrid;
        int finalHeightGrid = heightGrid;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(urlString);
                int totalWidth = finalWidthGrid * 128;
                int totalHeight = finalHeightGrid * 128;

                BufferedImage fullImage = ImageProcessor.getImage(url, totalWidth, totalHeight);

                if (fullImage == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(Component.text("Failed to download or process the image. Please check the URL.", NamedTextColor.RED));
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
                    player.sendMessage(Component.text("Your map(s) have been created!", NamedTextColor.GREEN));
                });

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("An error occurred: " + e.getMessage(), NamedTextColor.RED));
                });
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to create map", e);
            }
        });

        return true;
    }
}
