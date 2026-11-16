package com.smp.smptools.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigDefaultsTest {

    @Test
    public void testStatsSaveOnQuitMigrationWhenLegacyConfiguredFalse() {
        FileConfiguration config = new YamlConfiguration();
        config.set("features.stats.save-on-quit", false);

        ConfigDefaults.applyDefaults(config);

        assertFalse(config.getBoolean("stats.save-on-quit"),
                "Legacy features.stats.save-on-quit: false must be migrated to stats.save-on-quit");
    }

    @Test
    public void testStatsSaveOnQuitDefaultWhenNotConfigured() {
        FileConfiguration config = new YamlConfiguration();

        ConfigDefaults.applyDefaults(config);

        assertTrue(config.getBoolean("stats.save-on-quit"),
                "Default stats.save-on-quit should be true when no legacy setting is present");
    }
}
