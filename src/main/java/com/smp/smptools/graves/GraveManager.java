package com.smp.smptools.graves;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GraveManager implements Listener {

    private final SMPTools plugin;
    private final Map<Location, Grave> graves = new HashMap<>();
    private File gravesFile;
    private FileConfiguration gravesConfig;

    public GraveManager(SMPTools plugin) {
        this.plugin = plugin;
        setupGravesConfig();
        loadGraves();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("features.player-graves.enabled")) {
            return;
        }

        Player player = event.getEntity();
        List<ItemStack> drops = new ArrayList<>(event.getDrops());

        if (drops.isEmpty()) {
            return; // No need for a grave if no items dropped
        }

        // Remove drops from the event so they don't spawn on the ground
        event.getDrops().clear();

        Location deathLocation = player.getLocation().getBlock().getLocation(); // Snap to block

        // Ensure we don't overwrite an existing block if possible, or find a safe spot?
        // For simplicity, we'll just place it at the exact block location.
        // If it's air/liquid, great. If not, we might overwrite.
        // Let's try to find the nearest air block upwards if solid.
        if (deathLocation.getBlock().getType().isSolid()) {
            deathLocation.add(0, 1, 0);
        }

        String cause = player.getLastDamageCause() != null ? player.getLastDamageCause().getCause().name() : "UNKNOWN";
        Grave grave = new Grave(player.getUniqueId(), player.getName(), deathLocation, drops,
                System.currentTimeMillis(), cause);

        createGraveBlock(grave);
        graves.put(deathLocation, grave);
        saveGraves();

        player.sendMessage(Component.text("Your items have been stored in a grave at " +
                deathLocation.getBlockX() + ", " + deathLocation.getBlockY() + ", " + deathLocation.getBlockZ(),
                NamedTextColor.YELLOW));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.PLAYER_HEAD && block.getType() != Material.PLAYER_WALL_HEAD) {
            return;
        }

        Location location = block.getLocation();
        if (!graves.containsKey(location)) {
            return;
        }

        event.setCancelled(true); // Prevent normal interaction
        Grave grave = graves.get(location);
        Player player = event.getPlayer();

        // Loot the grave
        lootGrave(grave, player);
    }

    private void createGraveBlock(Grave grave) {
        Location loc = grave.getLocation();
        Block block = loc.getBlock();
        block.setType(Material.PLAYER_HEAD);

        if (block.getState() instanceof Skull skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(grave.getOwner()));
            skull.update();
        }

        spawnHologram(grave);
    }

    private void spawnHologram(Grave grave) {
        Location loc = grave.getLocation().clone().add(0.5, -0.5, 0.5); // Center above block
        double lineSpacing = 0.25;

        spawnArmorStand(loc.clone().add(0, lineSpacing * 3, 0),
                Component.text("R.I.P " + grave.getOwnerName(), NamedTextColor.RED));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        spawnArmorStand(loc.clone().add(0, lineSpacing * 2, 0), Component.text(
                "Died on " + formatter.format(Instant.ofEpochMilli(grave.getTimeOfDeath())), NamedTextColor.GRAY));

        spawnArmorStand(loc.clone().add(0, lineSpacing * 1, 0),
                Component.text("Cause: " + grave.getCauseOfDeath(), NamedTextColor.GRAY));

        spawnArmorStand(loc, Component.text("Items: " + grave.getItems().size(), NamedTextColor.YELLOW));
    }

    private void spawnArmorStand(Location loc, Component text) {
        ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        as.setVisible(false);
        as.setGravity(false);
        as.setCustomNameVisible(true);
        as.customName(text);
        as.setMarker(true);
        as.setSmall(true);
        // Tag it so we can remove it later
        as.addScoreboardTag("grave_hologram");
        as.addScoreboardTag("grave_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ());
    }

    private void lootGrave(Grave grave, Player looter) {
        Location loc = grave.getLocation();

        // Drop items
        for (ItemStack item : grave.getItems()) {
            if (item != null) {
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }

        // Remove block
        loc.getBlock().setType(Material.AIR);

        // Remove holograms
        loc.getWorld().getNearbyEntities(loc, 2, 2, 2).forEach(entity -> {
            if (entity instanceof ArmorStand && entity.getScoreboardTags()
                    .contains("grave_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ())) {
                entity.remove();
            }
        });

        graves.remove(loc);
        saveGraves();

        looter.sendMessage(
                Component.text("You have looted the grave of " + grave.getOwnerName(), NamedTextColor.GREEN));
    }

    private void setupGravesConfig() {
        gravesFile = new File(plugin.getDataFolder(), "graves.yml");
        if (!gravesFile.exists()) {
            try {
                gravesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create graves.yml file!");
            }
        }
        gravesConfig = YamlConfiguration.loadConfiguration(gravesFile);
    }

    private void saveGraves() {
        gravesConfig.set("graves", null); // Clear existing
        List<Map<String, Object>> gravesList = new ArrayList<>();

        for (Grave grave : graves.values()) {
            Map<String, Object> data = new HashMap<>();
            data.put("owner", grave.getOwner().toString());
            data.put("ownerName", grave.getOwnerName());
            data.put("location", grave.getLocation());
            data.put("items", grave.getItems());
            data.put("time", grave.getTimeOfDeath());
            data.put("cause", grave.getCauseOfDeath());
            gravesList.add(data);
        }

        gravesConfig.set("graves", gravesList);

        try {
            gravesConfig.save(gravesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save graves.yml!");
        }
    }

    @SuppressWarnings("unchecked")
    private void loadGraves() {
        if (!gravesConfig.contains("graves"))
            return;

        List<Map<?, ?>> list = gravesConfig.getMapList("graves");
        for (Map<?, ?> map : list) {
            try {
                UUID owner = UUID.fromString((String) map.get("owner"));
                String ownerName = (String) map.get("ownerName");
                Location location = (Location) map.get("location");
                List<ItemStack> items = (List<ItemStack>) map.get("items");
                long time = (long) map.get("time");
                String cause = (String) map.get("cause");

                Grave grave = new Grave(owner, ownerName, location, items, time, cause);
                graves.put(location, grave);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load a grave: " + e.getMessage());
            }
        }
    }
}
