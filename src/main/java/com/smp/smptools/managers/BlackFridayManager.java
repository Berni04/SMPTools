package com.smp.smptools.managers;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class BlackFridayManager {

    private final SMPTools plugin;
    private File configFile;
    private FileConfiguration config;

    public BlackFridayManager(SMPTools plugin) {
        this.plugin = plugin;
        setupConfig();
    }

    private void setupConfig() {
        configFile = new File(plugin.getDataFolder(), "blackfriday.yml");
        if (!configFile.exists()) {
            plugin.saveResource("blackfriday.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save blackfriday.yml: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", false);
    }

    public void setEnabled(boolean enabled) {
        config.set("enabled", enabled);
        saveConfig();
    }

    public int getDiscountPercentage() {
        return config.getInt("discount-percentage", 90);
    }

    public void broadcastToggle(boolean enabled) {
        String message = config.getString("announcement-message",
                "&6&l[Black Friday] &eVillager trades are now {status}!");
        String status = enabled ? "&aON SALE - " + getDiscountPercentage() + "% OFF!" : "&cdiscounted";
        message = message.replace("{status}", status);
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
