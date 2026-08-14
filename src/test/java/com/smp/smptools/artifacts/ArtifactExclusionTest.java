package com.smp.smptools.artifacts;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArtifactExclusionTest {

    @Test
    public void testExcludedFoodsContainsValuableAndHarmfulItems() {
        assertTrue(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.GOLDEN_APPLE));
        assertTrue(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.ENCHANTED_GOLDEN_APPLE));
        assertTrue(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.ROTTEN_FLESH));
        assertTrue(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.POISONOUS_POTATO));
        assertTrue(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.PUFFERFISH));
        assertTrue(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.SPIDER_EYE));
        assertTrue(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.CHORUS_FRUIT));
        assertTrue(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.SUSPICIOUS_STEW));

        // Normal foods should not be excluded
        assertFalse(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.COOKED_BEEF));
        assertFalse(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.BREAD));
        assertFalse(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.GOLDEN_CARROT));
        assertFalse(ArtifactListener.EXCLUDED_AUTO_FEED_FOODS.contains(Material.COOKED_PORKCHOP));
    }
}
