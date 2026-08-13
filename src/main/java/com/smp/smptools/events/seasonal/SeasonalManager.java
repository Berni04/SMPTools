package com.smp.smptools.events.seasonal;

import com.smp.smptools.SMPTools;
import com.smp.smptools.artifacts.ArtifactType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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

    public SeasonalManager(SMPTools plugin) {
        this.plugin = plugin;
        loadConfigurations();
    }

    public void loadConfigurations() {
        locationsFile = new File(plugin.getDataFolder(), "seasonal_locations.yml");
        if (!locationsFile.exists()) {
            try {
                locationsFile.createNewFile();
            } catch (IOException ignored) {}
        }
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);

        playerDataFile = new File(plugin.getDataFolder(), "player_seasonal.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException ignored) {}
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void saveLocations() {
        try {
            locationsConfig.save(locationsFile);
        } catch (IOException ignored) {}
    }

    public void savePlayerData() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException ignored) {}
    }

    /**
     * Determines the currently active season based on date or admin override.
     */
    public SeasonType getCurrentSeason() {
        if (forcedSeason != null) {
            return forcedSeason;
        }

        String tzStr = plugin.getSeasonalConfig().getString("seasonal.timezone", "Europe/Paris");
        ZoneId zone = ZoneId.of(tzStr);
        ZonedDateTime now = ZonedDateTime.now(zone);
        MonthDay currentDay = MonthDay.from(now);

        // Halloween: Oct 15 - Nov 2
        MonthDay hwStart = MonthDay.of(10, 15);
        MonthDay hwEnd = MonthDay.of(11, 2);
        if ((!currentDay.isBefore(hwStart) && !currentDay.isAfter(MonthDay.of(10, 31))) ||
            (!currentDay.isBefore(MonthDay.of(11, 1)) && !currentDay.isAfter(hwEnd))) {
            return SeasonType.HALLOWEEN;
        }

        // Black Friday: Nov 20 - Nov 30
        MonthDay bfStart = MonthDay.of(11, 20);
        MonthDay bfEnd = MonthDay.of(11, 30);
        if (!currentDay.isBefore(bfStart) && !currentDay.isAfter(bfEnd)) {
            return SeasonType.BLACK_FRIDAY;
        }

        // Christmas: Dec 1 - Jan 6
        MonthDay xmasStart = MonthDay.of(12, 1);
        MonthDay xmasEnd = MonthDay.of(1, 6);
        if (!currentDay.isBefore(xmasStart) || !currentDay.isAfter(xmasEnd)) {
            return SeasonType.CHRISTMAS;
        }

        // Easter: Mar 25 - Apr 25
        MonthDay easterStart = MonthDay.of(3, 25);
        MonthDay easterEnd = MonthDay.of(4, 25);
        if (!currentDay.isBefore(easterStart) && !currentDay.isAfter(easterEnd)) {
            return SeasonType.EASTER;
        }

        // Summer: Jul 1 - Aug 31
        MonthDay sumStart = MonthDay.of(7, 1);
        MonthDay sumEnd = MonthDay.of(8, 31);
        if (!currentDay.isBefore(sumStart) && !currentDay.isAfter(sumEnd)) {
            return SeasonType.SUMMER;
        }

        return SeasonType.NONE;
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
        UUID uuid = player.getUniqueId();
        List<Integer> found = new ArrayList<>(getFoundPumpkins(uuid));

        if (found.contains(pumpkinId)) {
            player.sendActionBar(MiniMessage.miniMessage().deserialize("<gray>You already collected Pumpkin #" + pumpkinId + "!</gray>"));
            return false;
        }

        found.add(pumpkinId);
        playerDataConfig.set("players." + uuid + ".halloween.found", found);
        savePlayerData();

        int total = plugin.getSeasonalConfig().getInt("seasonal.halloween.total_pumpkins", 20);

        // Visual & audio celebratory effects
        player.playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.05);
        player.sendActionBar(MiniMessage.miniMessage().deserialize(
                "<gold><b>🎃 Spooky Pumpkin #" + pumpkinId + " Discovered! (" + found.size() + "/" + total + " Found)</b></gold>"
        ));

        // Mini reward
        int diamonds = plugin.getSeasonalConfig().getInt("seasonal.halloween.mini_reward_diamonds", 2);
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, diamonds));
        player.giveExp(plugin.getSeasonalConfig().getInt("seasonal.halloween.mini_reward_xp", 50));

        return true;
    }

    public boolean claimHalloweenGrandReward(Player player) {
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
        savePlayerData();

        // Award Jack's Pumpkin Helmet Artifact
        if (plugin.getArtifactManager() != null) {
            ItemStack helmet = plugin.getArtifactManager().createArtifact(ArtifactType.JACKS_PUMPKIN_HELMET);
            player.getInventory().addItem(helmet);
        }

        // Award Diamonds & Sound
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 16));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.2);

        // Global Announcement
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                "<gold><b>🎃 [HALLOWEEN]</b></gold> <yellow><b>" + player.getName() + "</b> has found all 20 hidden Spooky Pumpkins and unlocked <b>Jack's Pumpkin Helmet</b>!</yellow>"
        ));

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
        UUID uuid = player.getUniqueId();
        List<Integer> found = new ArrayList<>(getFoundEggs(uuid));

        if (found.contains(eggId)) {
            player.sendActionBar(MiniMessage.miniMessage().deserialize("<gray>You already collected Easter Egg #" + eggId + "!</gray>"));
            return false;
        }

        found.add(eggId);
        playerDataConfig.set("players." + uuid + ".easter.found", found);
        savePlayerData();

        int total = plugin.getSeasonalConfig().getInt("seasonal.easter.total_eggs", 15);

        // Visual & audio celebratory effects
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.05);
        player.sendActionBar(MiniMessage.miniMessage().deserialize(
                "<green><b>🥚 Easter Egg #" + eggId + " Discovered! (" + found.size() + "/" + total + " Found)</b></green>"
        ));

        // Mini reward
        int carrots = plugin.getSeasonalConfig().getInt("seasonal.easter.mini_reward_golden_carrots", 8);
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_CARROT, carrots));
        player.giveExp(40);

        return true;
    }

    public boolean claimEasterGrandReward(Player player) {
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
        savePlayerData();

        // Award Chlorophyll Band Artifact
        if (plugin.getArtifactManager() != null) {
            ItemStack band = plugin.getArtifactManager().createArtifact(ArtifactType.CHLOROPHYLL_BAND);
            player.getInventory().addItem(band);
        }

        // Award Golden Carrots, Diamonds & Sound
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, 12));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_CARROT, 32));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.2);

        // Global Announcement
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                "<green><b>🐣 [EASTER]</b></green> <yellow><b>" + player.getName() + "</b> has found all 15 hidden Easter Eggs and claimed the <b>Chlorophyll Band</b>!</yellow>"
        ));

        return true;
    }

    // ==========================================
    // Location Management & Hit Tests
    // ==========================================

    public Integer getPumpkinIdAt(Location loc) {
        if (!locationsConfig.contains("halloween_pumpkins")) return null;
        for (String key : Objects.requireNonNull(locationsConfig.getConfigurationSection("halloween_pumpkins")).getKeys(false)) {
            String path = "halloween_pumpkins." + key;
            String world = locationsConfig.getString(path + ".world");
            int x = locationsConfig.getInt(path + ".x");
            int y = locationsConfig.getInt(path + ".y");
            int z = locationsConfig.getInt(path + ".z");

            if (loc.getWorld() != null && loc.getWorld().getName().equals(world) &&
                loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z) {
                try {
                    return Integer.parseInt(key);
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    public Integer getEggIdAt(Location loc) {
        if (!locationsConfig.contains("easter_eggs")) return null;
        for (String key : Objects.requireNonNull(locationsConfig.getConfigurationSection("easter_eggs")).getKeys(false)) {
            String path = "easter_eggs." + key;
            String world = locationsConfig.getString(path + ".world");
            int x = locationsConfig.getInt(path + ".x");
            int y = locationsConfig.getInt(path + ".y");
            int z = locationsConfig.getInt(path + ".z");

            if (loc.getWorld() != null && loc.getWorld().getName().equals(world) &&
                loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z) {
                try {
                    return Integer.parseInt(key);
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
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

    public String getPumpkinHint(int id) {
        return locationsConfig.getString("halloween_pumpkins." + id + ".hint", "Hidden somewhere in the world!");
    }

    public String getEggHint(int id) {
        return locationsConfig.getString("easter_eggs." + id + ".hint", "Hidden in nature biomes!");
    }
}
