package com.smp.smptools.chunkloaders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChunkLoaderManagerTest {

    @Test
    public void testParseCoordinatesHandlesMalformedStringsGracefully() {
        assertNull(ChunkLoaderManager.parseCoordinates(null));
        assertNull(ChunkLoaderManager.parseCoordinates(""));
        assertNull(ChunkLoaderManager.parseCoordinates("   "));
        assertNull(ChunkLoaderManager.parseCoordinates("invalid"));
        assertNull(ChunkLoaderManager.parseCoordinates("world;not_a_number;64;100"));
        assertNull(ChunkLoaderManager.parseCoordinates("world;100;bad;100"));
        assertNull(ChunkLoaderManager.parseCoordinates("world;100;64;bad"));
        assertNull(ChunkLoaderManager.parseCoordinates("world;100;64"));
        assertNull(ChunkLoaderManager.parseCoordinates("world;100;64;100;extra"));

        ChunkLoaderManager.ParsedLocation parsed = ChunkLoaderManager.parseCoordinates("world;100;64;200");
        assertNotNull(parsed);
        assertEquals("world", parsed.worldName);
        assertEquals(100, parsed.x);
        assertEquals(64, parsed.y);
        assertEquals(200, parsed.z);
    }
}
