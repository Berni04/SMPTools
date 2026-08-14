package com.smp.smptools.graves;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GraveManager implements Listener {

    private final SMPTools plugin;
    private final Map<Location, Grave> graves = new ConcurrentHashMap<>();
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

        Location baseLoc = player.getLocation().getBlock().getLocation();
        World world = baseLoc.getWorld();
        if (world == null) return;

        Location validLocation = null;
        int baseX = baseLoc.getBlockX();
        int baseZ = baseLoc.getBlockZ();
        int startY = Math.min(world.getMaxHeight() - 1, Math.max(world.getMinHeight(), baseLoc.getBlockY()));

        // Scan nearby vertical offsets (0, +1, -1, +2, -2, ..., +10, -10)
        for (int dy = 0; dy <= 10; dy++) {
            int[] candidates = dy == 0 ? new int[]{startY} : new int[]{startY + dy, startY - dy};
            for (int y : candidates) {
                if (y <= world.getMinHeight() || y >= world.getMaxHeight()) continue;
                Location checkLoc = new Location(world, baseX, y, baseZ);
                Block b = checkLoc.getBlock();
                Block below = checkLoc.clone().add(0, -1, 0).getBlock();
                if (b.getType().isAir() && !b.isLiquid() && !below.getType().isAir() && !below.isLiquid() && below.getType().isSolid()) {
                    validLocation = checkLoc;
                    break;
                }
            }
            if (validLocation != null) break;
        }

        // If not found near death location (e.g. mid-air death or void), fallback to top solid surface block
        if (validLocation == null) {
            int topY = world.getHighestBlockYAt(baseX, baseZ);
            if (topY >= world.getMinHeight() && topY < world.getMaxHeight() - 1) {
                Location topLoc = new Location(world, baseX, topY + 1, baseZ);
                Block topBlock = topLoc.getBlock();
                Block belowTop = new Location(world, baseX, topY, baseZ).getBlock();
                if (topBlock.getType().isAir() && !topBlock.isLiquid() && !belowTop.getType().isAir() && !belowTop.isLiquid() && belowTop.getType().isSolid()) {
                    validLocation = topLoc;
                }
            }
        }

        // If no dry solid ground spot is found nearby (e.g. drowning in ocean/river or void),
        // fallback to death location clamped within world height bounds so player items are always preserved
        if (validLocation == null) {
            validLocation = baseLoc.clone();
            if (validLocation.getBlockY() <= world.getMinHeight()) {
                validLocation.setY(world.getMinHeight() + 1);
            } else if (validLocation.getBlockY() >= world.getMaxHeight()) {
                validLocation.setY(world.getMaxHeight() - 1);
            }
        }

        // Remove drops from the event only after safe location is confirmed
        event.getDrops().clear();

        String cause = player.getLastDamageCause() != null ? player.getLastDamageCause().getCause().name() : "UNKNOWN";
        Grave grave = new Grave(player.getUniqueId(), player.getName(), validLocation, drops,
                System.currentTimeMillis(), cause);

        createGraveBlock(grave);
        graves.put(validLocation, grave);
        saveGraves();

        String graveLocation = validLocation.getBlockX() + ", " + validLocation.getBlockY() + ", " + validLocation.getBlockZ();
        player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("grave.stored", player, Map.of("location", graveLocation)));
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

        looter.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("grave.looted", looter, Map.of("player", grave.getOwnerName())));
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
