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
}
