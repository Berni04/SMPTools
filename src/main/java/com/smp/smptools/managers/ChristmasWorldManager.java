package com.smp.smptools.managers;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ChristmasWorldManager {

    private final SMPTools plugin;
    private World christmasWorld;
    private static final String WORLD_NAME = "christmas";

    public ChristmasWorldManager(SMPTools plugin) {
        this.plugin = plugin;
        loadChristmasWorld();
    }

    public void loadChristmasWorld() {
        WorldCreator creator = new WorldCreator(WORLD_NAME);
        // Use a custom chunk generator to force snowy biome if generating for the first
        // time
        creator.generator(new SnowyChunkGenerator());

        this.christmasWorld = creator.createWorld();

        if (this.christmasWorld != null) {
            setupGameRules();
            plugin.getLogger().info("Christmas world loaded successfully!");
        } else {
            plugin.getLogger().severe("Failed to load Christmas world!");
        }
    }

    private void setupGameRules() {
        // Always Night (Midnight)
        christmasWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        christmasWorld.setTime(18000);

        // Always Storming/Snowing
        christmasWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        christmasWorld.setStorm(true);
        christmasWorld.setThundering(false);
        christmasWorld.setWeatherDuration(Integer.MAX_VALUE);

        // No Mob Spawning
        christmasWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
    }

    public World getChristmasWorld() {
        return christmasWorld;
    }

    // Custom ChunkGenerator to force Snowy Plains biome
    private static class SnowyChunkGenerator extends ChunkGenerator {
        @Override
        public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
            return new BiomeProvider() {
                @Override
                public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
                    return Biome.SNOWY_PLAINS;
                }

                @Override
                public List<Biome> getBiomes(WorldInfo worldInfo) {
                    return Collections.singletonList(Biome.SNOWY_PLAINS);
                }
            };
        }

        @Override
        public boolean shouldGenerateNoise() {
            return true;
        }

        @Override
        public boolean shouldGenerateSurface() {
            return true;
        }

        @Override
        public boolean shouldGenerateCaves() {
            return true;
        }

        @Override
        public boolean shouldGenerateDecorations() {
            return true;
        }

        @Override
        public boolean shouldGenerateMobs() {
            return false;
        }

        @Override
        public boolean shouldGenerateStructures() {
            return true;
        }
    }
}
