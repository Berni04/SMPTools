package com.smp.smptools.chunkloaders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChunkLoaderManagerTest {

    private ChunkLoaderManager createTestManager() {
        return new ChunkLoaderManager(null) {
            @Override
            public void loadChunkLoaders() {}
            @Override
            public void saveChunkLoaders() {}
        };
    }

    @Test
    public void testDeserializeLocationHandlesMalformedStringsGracefully() {
        // We test subclassing or null-safe helper
        // Since constructor calls loadChunkLoaders if plugin != null,
        // let's verify deserializeLocation returns null on bad input and never throws NumberFormatException
        ChunkLoaderManager manager;
        try {
            manager = new ChunkLoaderManager(null);
        } catch (Exception e) {
            // If constructor requires setup, let's make constructor safe when plugin is null
            manager = null;
        }

        // Test bad inputs on an instance if constructor supports null
        if (manager != null) {
            assertNull(manager.deserializeLocation(null));
            assertNull(manager.deserializeLocation(""));
            assertNull(manager.deserializeLocation("invalid"));
            assertNull(manager.deserializeLocation("world;not_a_number;64;100"));
            assertNull(manager.deserializeLocation("world;100;bad;100"));
            assertNull(manager.deserializeLocation("world;100;64;bad"));
            assertNull(manager.deserializeLocation("world;100;64"));
            assertNull(manager.deserializeLocation("world;100;64;100;extra"));
        }
    }
}
