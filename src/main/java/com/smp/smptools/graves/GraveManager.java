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

    public static final org.bukkit.NamespacedKey GRAVE_OWNER_KEY = new org.bukkit.NamespacedKey(SMPTools.getInstance(), "grave_owner");

    public GraveManager(SMPTools plugin) {
        this.plugin = plugin;
        setupGravesConfig();
        loadGraves();
        startGraveExpiryTask();
    }

    private void startGraveExpiryTask() {
        if (plugin == null || !plugin.isEnabled()) return;
        long expireHours = plugin.getConfig().getLong("features.player-graves.expire-hours", 72);
        if (expireHours <= 0) return;

        long expireMillis = expireHours * 3600_000L;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            List<Location> expired = new ArrayList<>();
            for (Map.Entry<Location, Grave> entry : graves.entrySet()) {
                if (now - entry.getValue().getTimeOfDeath() > expireMillis) {
                    expired.add(entry.getKey());
                }
            }
            for (Location loc : expired) {
                Grave g = graves.get(loc);
                if (g != null) {
                    // Expire grave: drop items and clear holograms
                    for (ItemStack item : g.getItems()) {
                        if (item != null && item.getType() != Material.AIR) {
                            loc.getWorld().dropItemNaturally(loc, item);
                        }
                    }
                    Block b = loc.getBlock();
                    if (b.getState() instanceof org.bukkit.block.Skull skull &&
                        skull.getPersistentDataContainer().has(GRAVE_OWNER_KEY, org.bukkit.persistence.PersistentDataType.STRING)) {
                        b.setType(Material.AIR);
                    }
                    removeHolograms(loc);
                    graves.remove(loc);
                }
            }
            if (!expired.isEmpty()) {
                saveGraves();
            }
        }, 1200L, 1200L); // Check every minute
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
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
                if (graves.containsKey(checkLoc)) continue; // Avoid grave collision
                Block b = checkLoc.getBlock();
                Block below = checkLoc.clone().add(0, -1, 0).getBlock();
                if (b.getType().isAir() && !b.isLiquid() && !below.getType().isAir() && !below.isLiquid() && below.getType().isSolid()) {
                    validLocation = checkLoc;
                    break;
                }
            }
            if (validLocation != null) break;
        }

        // If not found near death location, search upward for the first unoccupied dry air block
        if (validLocation == null) {
            int topY = world.getHighestBlockYAt(baseX, baseZ);
            int searchStartY = Math.max(world.getMinHeight() + 1, Math.min(startY, topY));
            for (int y = searchStartY; y < world.getMaxHeight() - 1; y++) {
                Location candidate = new Location(world, baseX, y, baseZ);
                if (graves.containsKey(candidate)) continue;
                Block candBlock = candidate.getBlock();
                if (candBlock.getType().isAir() && !candBlock.isLiquid()) {
                    validLocation = candidate;
                    break;
                }
            }
        }

        // Clamped fallback with horizontal search if column is full
        if (validLocation == null || graves.containsKey(validLocation)) {
            int topY = world.getHighestBlockYAt(baseX, baseZ);
            int clampedY = Math.max(world.getMinHeight() + 1, Math.min(world.getMaxHeight() - 1, topY + 1));
            Location candidate = new Location(world, baseX, clampedY, baseZ);
            while (graves.containsKey(candidate) && candidate.getBlockY() < world.getMaxHeight() - 1) {
                candidate.add(0, 1, 0);
            }
            if (!graves.containsKey(candidate)) {
                validLocation = candidate;
            } else {
                for (int dx = -2; dx <= 2 && (validLocation == null || graves.containsKey(validLocation)); dx++) {
                    for (int dz = -2; dz <= 2 && (validLocation == null || graves.containsKey(validLocation)); dz++) {
                        int hx = baseX + dx;
                        int hz = baseZ + dz;
                        int hy = Math.max(world.getMinHeight() + 1, Math.min(world.getMaxHeight() - 1, world.getHighestBlockYAt(hx, hz) + 1));
                        Location hCand = new Location(world, hx, hy, hz);
                        if (!graves.containsKey(hCand)) {
                            validLocation = hCand;
                        }
                    }
                }
            }
        }

        if (validLocation == null || graves.containsKey(validLocation)) {
            return;
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

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || (block.getType() != Material.PLAYER_HEAD && block.getType() != Material.PLAYER_WALL_HEAD)) {
            return;
        }

        Location location = block.getLocation();
        if (!graves.containsKey(location)) {
            return;
        }

        event.setCancelled(true); // Prevent normal interaction
        Grave grave = graves.get(location);
        Player player = event.getPlayer();

        if (!player.getUniqueId().equals(grave.getOwner()) && !player.hasPermission("smptools.graves.admin")) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>🔒 You cannot open someone else's grave!</red>"));
            return;
        }

        // Loot the grave
        lootGrave(grave, player);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!graves.containsKey(loc)) return;

        Grave grave = graves.get(loc);
        Player player = event.getPlayer();
        if (!player.getUniqueId().equals(grave.getOwner()) && !player.hasPermission("smptools.graves.admin")) {
            event.setCancelled(true);
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>🔒 You cannot break someone else's grave!</red>"));
            return;
        }

        event.setCancelled(true);
        lootGrave(grave, player);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        event.blockList().removeIf(graves::containsKey);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().removeIf(b -> graves.containsKey(b.getLocation()));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (graves.containsKey(b.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (graves.containsKey(b.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onLiquidFlow(org.bukkit.event.block.BlockFromToEvent event) {
        if (graves.containsKey(event.getToBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    private void createGraveBlock(Grave grave) {
        Location loc = grave.getLocation();
        Block block = loc.getBlock();
        block.setType(Material.PLAYER_HEAD);

        if (block.getState() instanceof Skull skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(grave.getOwner()));
            skull.getPersistentDataContainer().set(GRAVE_OWNER_KEY, org.bukkit.persistence.PersistentDataType.STRING, grave.getOwner().toString());
            skull.update();
        }

        spawnHologram(grave);
    }

    private void removeHolograms(Location loc) {
        if (loc.getWorld() == null) return;
        String graveTag = "grave_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        loc.getWorld().getNearbyEntities(loc, 3, 3, 3).forEach(entity -> {
            if (entity instanceof ArmorStand && entity.getScoreboardTags().contains(graveTag)) {
                entity.remove();
            }
        });
    }

    private void spawnHologram(Grave grave) {
        Location baseLoc = grave.getLocation();
        String graveTag = "grave_" + baseLoc.getBlockX() + "_" + baseLoc.getBlockY() + "_" + baseLoc.getBlockZ();
        Location loc = baseLoc.clone().add(0.5, -0.5, 0.5); // Center above block
        double lineSpacing = 0.25;

        spawnArmorStand(loc.clone().add(0, lineSpacing * 3, 0),
                Component.text("R.I.P " + grave.getOwnerName(), NamedTextColor.RED), graveTag);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        spawnArmorStand(loc.clone().add(0, lineSpacing * 2, 0), Component.text(
                "Died on " + formatter.format(Instant.ofEpochMilli(grave.getTimeOfDeath())), NamedTextColor.GRAY), graveTag);

        spawnArmorStand(loc.clone().add(0, lineSpacing * 1, 0),
                Component.text("Cause: " + grave.getCauseOfDeath(), NamedTextColor.GRAY), graveTag);

        spawnArmorStand(loc, Component.text("Items: " + grave.getItems().size(), NamedTextColor.YELLOW), graveTag);
    }

    private void spawnArmorStand(Location loc, Component text, String graveTag) {
        ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        as.setVisible(false);
        as.setGravity(false);
        as.setCustomNameVisible(true);
        as.customName(text);
        as.setMarker(true);
        as.setSmall(true);
        as.setInvulnerable(true);
        // Tag it so we can remove it later
        as.addScoreboardTag("grave_hologram");
        as.addScoreboardTag(graveTag);
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
        removeHolograms(loc);

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
            com.smp.smptools.utils.AtomicFileWriter.save(gravesConfig, gravesFile);
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
