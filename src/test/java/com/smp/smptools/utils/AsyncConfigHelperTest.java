package com.smp.smptools.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncConfigHelperTest {

    @Test
    public void testSaveConfigAsyncAndShutdown(@TempDir Path tempDir) throws IOException {
        File configFile = tempDir.resolve("test_config.yml").toFile();
        FileConfiguration config = new YamlConfiguration();
        config.set("key1", "value1");
        config.set("key2", 42);

        AsyncConfigHelper.saveConfigAsync(null, config, configFile, "test_config.yml");
        AsyncConfigHelper.shutdown();

        assertTrue(configFile.exists(), "Config file should exist after save and shutdown");
        String content = Files.readString(configFile.toPath());
        assertTrue(content.contains("key1: value1"));
        assertTrue(content.contains("key2: 42"));
    }

    @Test
    public void testSaveConfigAsyncRecreationAfterShutdown(@TempDir Path tempDir) throws IOException {
        File configFile1 = tempDir.resolve("config1.yml").toFile();
        FileConfiguration config1 = new YamlConfiguration();
        config1.set("step", 1);

        AsyncConfigHelper.saveConfigAsync(null, config1, configFile1, "config1.yml");
        AsyncConfigHelper.shutdown();

        assertTrue(configFile1.exists());

        File configFile2 = tempDir.resolve("config2.yml").toFile();
        FileConfiguration config2 = new YamlConfiguration();
        config2.set("step", 2);

        // Subsequent saveConfigAsync after shutdown should re-initialize executor without falling back
        AsyncConfigHelper.saveConfigAsync(null, config2, configFile2, "config2.yml");
        AsyncConfigHelper.shutdown();

        assertTrue(configFile2.exists());
        String content2 = Files.readString(configFile2.toPath());
        assertTrue(content2.contains("step: 2"));
    }
}
