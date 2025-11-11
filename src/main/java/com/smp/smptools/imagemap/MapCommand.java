package com.smp.smptools.imagemap;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /tomap <url>");
            return true;
        }

        Player player = (Player) sender;
        String urlString = args[0];

        player.sendMessage(ChatColor.GRAY + "Downloading and processing image... this may take a moment.");

        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                BufferedImage image = ImageProcessor.getImage(url);

                if (image == null) {
                    player.sendMessage(ChatColor.RED + "Failed to download or process the image. Please check the URL.");
                    return;
                }

                // Process and render the image on the main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    MapView mapView = Bukkit.createMap(player.getWorld());
                    mapView.getRenderers().forEach(mapView::removeRenderer); // Clear default renderers

                    mapView.addRenderer(new ImageMapRenderer(image));

                    ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                    MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
                    mapMeta.setMapView(mapView);
                    mapItem.setItemMeta(mapMeta);

                    // Save for persistence
                    plugin.getImageMapsConfig().set("maps." + mapView.getId(), urlString);
                    plugin.saveImageMapsConfig();

                    player.getInventory().addItem(mapItem);
                    player.sendMessage(ChatColor.GREEN + "Your map has been created!");
                });

            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "An error occurred: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        return true;
    }
}
