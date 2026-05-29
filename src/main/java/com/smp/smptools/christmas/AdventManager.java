package com.smp.smptools.christmas;

import com.smp.smptools.SMPTools;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class AdventManager {

    private final SMPTools plugin;
    private File configFile;
    private FileConfiguration config;
    private static final ZoneId CET_ZONE = ZoneId.of("Europe/Paris");

    public AdventManager(SMPTools plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "advent.yml");
        if (!configFile.exists()) {
            plugin.saveResource("advent.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save advent.yml!", e);
        }
    }

    public List<String> getRewards(int day) {
        return config.getStringList("rewards." + day);
    }

    public boolean hasClaimed(UUID playerUUID, int day) {
        List<Integer> claimedDays = config.getIntegerList("players." + playerUUID.toString());
        return claimedDays.contains(day);
    }

    public void setClaimed(UUID playerUUID, int day) {
        List<Integer> claimedDays = config.getIntegerList("players." + playerUUID.toString());
        if (!claimedDays.contains(day)) {
            claimedDays.add(day);
            config.set("players." + playerUUID.toString(), claimedDays);
            saveConfig();
        }
    }

    public boolean isDecember() {
        ZonedDateTime now = ZonedDateTime.now(CET_ZONE);
        return now.getMonthValue() == 12;
    }

    public int getCurrentDay() {
        ZonedDateTime now = ZonedDateTime.now(CET_ZONE);
        return now.getDayOfMonth();
    }
}
