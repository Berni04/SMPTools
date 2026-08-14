package com.smp.smptools.events.seasonal;

import com.smp.smptools.SMPTools;
import com.smp.smptools.artifacts.ArtifactType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;

/**
 * Core manager for seasonal event dates, scavenger hunt locations, and player progress.
 */
public class SeasonalManager {

    private final SMPTools plugin;
    private SeasonType forcedSeason = null;

    private File locationsFile;
    private FileConfiguration locationsConfig;

    private File playerDataFile;
    private FileConfiguration playerDataConfig;

    private ZoneId cachedZoneId = ZoneId.of("Europe/Paris");
    private final Map<SeasonType, MonthDay[]> cachedDateRanges = new EnumMap<>(SeasonType.class);

    private final Map<String, Integer> pumpkinLocations = new HashMap<>();
    private final Map<String, Integer> eggLocations = new HashMap<>();

    public SeasonalManager(SMPTools plugin) {
        this.plugin = plugin;
        loadConfigurations();
    }

    public void loadConfigurations() {
        locationsFile = new File(plugin.getDataFolder(), "seasonal_locations.yml");
        if (!locationsFile.exists()) {
            try {
                locationsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create seasonal_locations.yml", e);
            }
        }
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);

        playerDataFile = new File(plugin.getDataFolder(), "player_seasonal.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create player_seasonal.yml", e);
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);

        // Cache timezone
        String tzStr = plugin.getSeasonalConfig().getString("seasonal.timezone", "Europe/Paris");
        try {
            cachedZoneId = ZoneId.of(tzStr);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid seasonal.timezone '" + tzStr + "', defaulting to Europe/Paris");
            cachedZoneId = ZoneId.of("Europe/Paris");
        }

        // Cache date ranges
        FileConfiguration cfg = plugin.getSeasonalConfig();
        cacheDateRange(SeasonType.HALLOWEEN, cfg.getString("seasonal.dates.halloween.start", "10-15"), cfg.getString("seasonal.dates.halloween.end", "11-02"));
        cacheDateRange(SeasonType.BLACK_FRIDAY, cfg.getString("seasonal.dates.black_friday.start", "11-20"), cfg.getString("seasonal.dates.black_friday.end", "11-30"));
        cacheDateRange(SeasonType.CHRISTMAS, cfg.getString("seasonal.dates.christmas.start", "12-01"), cfg.getString("seasonal.dates.christmas.end", "01-06"));
        cacheDateRange(SeasonType.EASTER, cfg.getString("seasonal.dates.easter.start", "03-25"), cfg.getString("seasonal.dates.easter.end", "04-25"));
        cacheDateRange(SeasonType.SUMMER, cfg.getString("seasonal.dates.summer.start", "07-01"), cfg.getString("seasonal.dates.summer.end", "08-31"));

        // Cache target locations in memory
        reloadLocationCaches();
    }

    private void reloadLocationCaches() {
        pumpkinLocations.clear();
        eggLocations.clear();

        if (locationsConfig.isConfigurationSection("halloween_pumpkins")) {
            ConfigurationSection sec = locationsConfig.getConfigurationSection("halloween_pumpkins");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    try {
                        int id = Integer.parseInt(key);
                        String path = "halloween_pumpkins." + key;
                        String world = locationsConfig.getString(path + ".world");
                        int x = locationsConfig.getInt(path + ".x");
                        int y = locationsConfig.getInt(path + ".y");
                        int z = locationsConfig.getInt(path + ".z");
                        if (world != null) {
                            pumpkinLocations.put(locKey(world, x, y, z), id);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (locationsConfig.isConfigurationSection("easter_eggs")) {
            ConfigurationSection sec = locationsConfig.getConfigurationSection("easter_eggs");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    try {
                        int id = Integer.parseInt(key);
                        String path = "easter_eggs." + key;
                        String world = locationsConfig.getString(path + ".world");
                        int x = locationsConfig.getInt(path + ".x");
                        int y = locationsConfig.getInt(path + ".y");
                        int z = locationsConfig.getInt(path + ".z");
                        if (world != null) {
                            eggLocations.put(locKey(world, x, y, z), id);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    private String locKey(String world, int x, int y, int z) {
        return world + ":" + x + "," + y + "," + z;
    }

    private void cacheDateRange(SeasonType season, String startStr, String endStr) {
        try {
            String[] startParts = startStr.split("-");
            String[] endParts = endStr.split("-");
            MonthDay start = MonthDay.of(Integer.parseInt(startParts[0]), Integer.parseInt(startParts[1]));
            MonthDay end = MonthDay.of(Integer.parseInt(endParts[0]), Integer.parseInt(endParts[1]));
            cachedDateRanges.put(season, new MonthDay[]{start, end});
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse seasonal date range for " + season + " [" + startStr + " - " + endStr + "]: " + e.getMessage());
        }
    }

    public boolean saveLocations() {
        try {
            locationsConfig.save(locationsFile);
            reloadLocationCaches();
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save seasonal_locations.yml", e);
            return false;
        }
    }

    public boolean savePlayerData() {
        try {
            playerDataConfig.save(playerDataFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player_seasonal.yml", e);
            return false;
        }
    }

    /**
     * Determines the currently active season based on date or admin override.
     */
    public SeasonType getCurrentSeason() {
        if (forcedSeason != null) {
            return forcedSeason;
        }

        ZonedDateTime now = ZonedDateTime.now(cachedZoneId);
        MonthDay currentDay = MonthDay.from(now);

        for (SeasonType season : new SeasonType[]{SeasonType.HALLOWEEN, SeasonType.BLACK_FRIDAY, SeasonType.CHRISTMAS, SeasonType.EASTER, SeasonType.SUMMER}) {
            MonthDay[] range = cachedDateRanges.get(season);
            if (range != null && isDateInRange(currentDay, range[0], range[1])) {
                return season;
            }
        }

        return SeasonType.NONE;
    }

    private boolean isDateInRange(MonthDay current, MonthDay start, MonthDay end) {
        if (start.isAfter(end)) {
            // Cross-year date range (e.g. Dec 1 to Jan 6)
            return !current.isBefore(start) || !current.isAfter(end);
        } else {
            return !current.isBefore(start) && !current.isAfter(end);
        }
    }

    public void setForcedSeason(SeasonType season) {
        this.forcedSeason = season;
    }

    public boolean isSeasonActive(SeasonType type) {
        return getCurrentSeason() == type;
    }

    // ==========================================
    // Halloween Scavenger Hunt
    // ==========================================

    public List<Integer> getFoundPumpkins(UUID playerUuid) {
        return playerDataConfig.getIntegerList("players." + playerUuid + ".halloween.found");
    }

    public boolean hasClaimedHalloweenGrand(UUID playerUuid) {
        return playerDataConfig.getBoolean("players." + playerUuid + ".halloween.claimed_grand", false);
    }

    public boolean discoverPumpkin(Player player, int pumpkinId) {
        if (!isSeasonActive(SeasonType.HALLOWEEN)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Halloween season is not currently active!</red>"));
            return false;
        }

        UUID uuid = player.getUniqueId();
        List<Integer> found = new ArrayList<>(getFoundPumpkins(uuid));

        if (found.contains(pumpkinId)) {
            player.sendActionBar(MiniMessage.miniMessage().deserialize("<gray>You already collected Pumpkin #" + pumpkinId + "!</gray>"));
            return false;
        }

        List<Integer> backup = new ArrayList<>(found);
        found.add(pumpkinId);
        playerDataConfig.set("players." + uuid + ".halloween.found", found);
        if (!savePlayerData()) {
            playerDataConfig.set("players." + uuid + ".halloween.found", backup);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to save pumpkin discovery! Please try again.</red>"));
            return false;
        }

        int total = plugin.getSeasonalConfig().getInt("seasonal.halloween.total_pumpkins", 20);

        // Visual & audio celebratory effects
        player.playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.05);
        player.sendActionBar(MiniMessage.miniMessage().deserialize(
                "<gold><b>🎃 Spooky Pumpkin #" + pumpkinId + " Discovered! (" + found.size() + "/" + total + " Found)</b></gold>"
        ));

        // Mini reward
        int diamonds = plugin.getSeasonalConfig().getInt("seasonal.halloween.mini_reward_diamonds", 2);
        if (diamonds > 0) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(Material.DIAMOND, diamonds));
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        player.giveExp(plugin.getSeasonalConfig().getInt("seasonal.halloween.mini_reward_xp", 50));

        return true;
    }

    public boolean claimHalloweenGrandReward(Player player) {
        if (!isSeasonActive(SeasonType.HALLOWEEN)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Halloween season is not currently active!</red>"));
            return false;
        }

        UUID uuid = player.getUniqueId();
        int total = plugin.getSeasonalConfig().getInt("seasonal.halloween.total_pumpkins", 20);
        List<Integer> found = getFoundPumpkins(uuid);

        if (found.size() < total) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have only found " + found.size() + "/" + total + " pumpkins!</red>"));
            return false;
        }

        if (hasClaimedHalloweenGrand(uuid)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You already claimed the Halloween Grand Reward!</red>"));
            return false;
        }

        playerDataConfig.set("players." + uuid + ".halloween.claimed_grand", true);
        if (!savePlayerData()) {
            playerDataConfig.set("players." + uuid + ".halloween.claimed_grand", false);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to save reward claim status! Please contact an administrator.</red>"));
            return false;
        }

        boolean artifactsEnabled = plugin.getArtifactManager() != null && plugin.getConfig().getBoolean("features.artifacts.enabled", true);

        // Award Jack's Pumpkin Helmet Artifact or extra diamonds
        if (artifactsEnabled) {
            ItemStack helmet = plugin.getArtifactManager().createArtifact(ArtifactType.JACKS_PUMPKIN_HELMET);
            Map<Integer, ItemStack> helmetLeftover = player.getInventory().addItem(helmet);
            for (ItemStack drop : helmetLeftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        } else {
            Map<Integer, ItemStack> extraDia = player.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));
            for (ItemStack drop : extraDia.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        // Award Base Diamonds & Sound
        Map<Integer, ItemStack> diaLeftover = player.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));
        for (ItemStack drop : diaLeftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.2);

        // Global Announcement using dynamic configured total
        if (artifactsEnabled) {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                    "<gold><b>🎃 [HALLOWEEN]</b></gold> <yellow><b>" + player.getName() + "</b> has found all " + total + " hidden Spooky Pumpkins and unlocked <b>Jack's Pumpkin Helmet</b>!</yellow>"
            ));
        } else {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                    "<gold><b>🎃 [HALLOWEEN]</b></gold> <yellow><b>" + player.getName() + "</b> has found all " + total + " hidden Spooky Pumpkins and unlocked the <b>Halloween Grand Reward</b>!</yellow>"
            ));
        }

        return true;
    }

    // ==========================================
    // Easter Scavenger Hunt
    // ==========================================

    public List<Integer> getFoundEggs(UUID playerUuid) {
        return playerDataConfig.getIntegerList("players." + playerUuid + ".easter.found");
    }

    public boolean hasClaimedEasterGrand(UUID playerUuid) {
        return playerDataConfig.getBoolean("players." + playerUuid + ".easter.claimed_grand", false);
    }

    public boolean discoverEgg(Player player, int eggId) {
        if (!isSeasonActive(SeasonType.EASTER)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Easter season is not currently active!</red>"));
            return false;
        }

        UUID uuid = player.getUniqueId();
        List<Integer> found = new ArrayList<>(getFoundEggs(uuid));

        if (found.contains(eggId)) {
            player.sendActionBar(MiniMessage.miniMessage().deserialize("<gray>You already collected Easter Egg #" + eggId + "!</gray>"));
            return false;
        }

        List<Integer> backup = new ArrayList<>(found);
        found.add(eggId);
        playerDataConfig.set("players." + uuid + ".easter.found", found);
        if (!savePlayerData()) {
            playerDataConfig.set("players." + uuid + ".easter.found", backup);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to save Easter egg discovery! Please try again.</red>"));
            return false;
        }

        int total = plugin.getSeasonalConfig().getInt("seasonal.easter.total_eggs", 15);

        // Visual & audio celebratory effects
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.05);
        player.sendActionBar(MiniMessage.miniMessage().deserialize(
                "<green><b>🥚 Easter Egg #" + eggId + " Discovered! (" + found.size() + "/" + total + " Found)</b></green>"
        ));

        // Mini rewards: Carrots & Diamonds
        int carrots = plugin.getSeasonalConfig().getInt("seasonal.easter.mini_reward_golden_carrots", 8);
        int diamonds = plugin.getSeasonalConfig().getInt("seasonal.easter.mini_reward_diamonds", 1);

        if (carrots > 0) {
            Map<Integer, ItemStack> cLeft = player.getInventory().addItem(new ItemStack(Material.GOLDEN_CARROT, carrots));
            for (ItemStack drop : cLeft.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        if (diamonds > 0) {
            Map<Integer, ItemStack> dLeft = player.getInventory().addItem(new ItemStack(Material.DIAMOND, diamonds));
            for (ItemStack drop : dLeft.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        player.giveExp(40);

        return true;
    }

    public boolean claimEasterGrandReward(Player player) {
        if (!isSeasonActive(SeasonType.EASTER)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Easter season is not currently active!</red>"));
            return false;
        }

        UUID uuid = player.getUniqueId();
        int total = plugin.getSeasonalConfig().getInt("seasonal.easter.total_eggs", 15);
        List<Integer> found = getFoundEggs(uuid);

        if (found.size() < total) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have only found " + found.size() + "/" + total + " Easter Eggs!</red>"));
            return false;
        }

        if (hasClaimedEasterGrand(uuid)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You already claimed the Easter Grand Reward!</red>"));
            return false;
        }

        playerDataConfig.set("players." + uuid + ".easter.claimed_grand", true);
        if (!savePlayerData()) {
            playerDataConfig.set("players." + uuid + ".easter.claimed_grand", false);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to save reward claim status! Please contact an administrator.</red>"));
            return false;
        }

        boolean artifactsEnabled = plugin.getArtifactManager() != null && plugin.getConfig().getBoolean("features.artifacts.enabled", true);

        // Award Chlorophyll Band Artifact if artifacts enabled, else extra diamonds
        if (artifactsEnabled) {
            ItemStack band = plugin.getArtifactManager().createArtifact(ArtifactType.CHLOROPHYLL_BAND);
            Map<Integer, ItemStack> bandLeft = player.getInventory().addItem(band);
            for (ItemStack drop : bandLeft.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        } else {
            Map<Integer, ItemStack> extraDia = player.getInventory().addItem(new ItemStack(Material.DIAMOND, 12));
            for (ItemStack drop : extraDia.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        // Award Golden Carrots, Diamonds & Sound
        Map<Integer, ItemStack> diaLeft = player.getInventory().addItem(new ItemStack(Material.DIAMOND, 12));
        for (ItemStack drop : diaLeft.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        Map<Integer, ItemStack> carrotLeft = player.getInventory().addItem(new ItemStack(Material.GOLDEN_CARROT, 32));
        for (ItemStack drop : carrotLeft.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.2);

        // Global Announcement using dynamic configured total
        if (artifactsEnabled) {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                    "<green><b>🐣 [EASTER]</b></green> <yellow><b>" + player.getName() + "</b> has found all " + total + " hidden Easter Eggs and claimed the <b>Chlorophyll Band</b>!</yellow>"
            ));
        } else {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                    "<green><b>🐣 [EASTER]</b></green> <yellow><b>" + player.getName() + "</b> has found all " + total + " hidden Easter Eggs and claimed the <b>Easter Grand Reward</b>!</yellow>"
            ));
        }

        return true;
    }

    // ==========================================
    // Location Management & Hit Tests
    // ==========================================

    public Integer getPumpkinIdAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return pumpkinLocations.get(locKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    public Integer getEggIdAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return eggLocations.get(locKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    public void setPumpkinLocation(int id, Location loc, String hint) {
        String path = "halloween_pumpkins." + id;
        locationsConfig.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        locationsConfig.set(path + ".x", loc.getBlockX());
        locationsConfig.set(path + ".y", loc.getBlockY());
        locationsConfig.set(path + ".z", loc.getBlockZ());
        locationsConfig.set(path + ".hint", hint);
        saveLocations();
    }

    public void setEggLocation(int id, Location loc, String hint) {
        String path = "easter_eggs." + id;
        locationsConfig.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        locationsConfig.set(path + ".x", loc.getBlockX());
        locationsConfig.set(path + ".y", loc.getBlockY());
        locationsConfig.set(path + ".z", loc.getBlockZ());
        locationsConfig.set(path + ".hint", hint);
        saveLocations();
    }

    public void removePumpkinLocation(int id) {
        locationsConfig.set("halloween_pumpkins." + id, null);
        saveLocations();
    }

    public void removeEggLocation(int id) {
        locationsConfig.set("easter_eggs." + id, null);
        saveLocations();
    }

    public String getPumpkinHint(int id) {
        return locationsConfig.getString("halloween_pumpkins." + id + ".hint", "Hidden somewhere in the world!");
    }

    public String getEggHint(int id) {
        return locationsConfig.getString("easter_eggs." + id + ".hint", "Hidden in nature biomes!");
    }
}
