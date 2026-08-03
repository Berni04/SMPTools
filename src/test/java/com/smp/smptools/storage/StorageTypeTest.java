package com.smp.smptools.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StorageTypeTest {

    @Test
    public void testParseValidTypes() {
        assertEquals(StorageType.FLATFILE, StorageType.parse("FLATFILE"));
        assertEquals(StorageType.SQLITE, StorageType.parse("sqlite"));
        assertEquals(StorageType.MYSQL, StorageType.parse("MySQL"));
        assertEquals(StorageType.MARIADB, StorageType.parse("MARIADB"));
        assertEquals(StorageType.MONGODB, StorageType.parse("MongoDB"));
    }

    @Test
    public void testParseInvalidTypesDefaultToFlatFile() {
        assertEquals(StorageType.FLATFILE, StorageType.parse(null));
        assertEquals(StorageType.SQLITE, StorageType.parse("SQLITE"));
        assertEquals(StorageType.FLATFILE, StorageType.parse("UNKNOWN_DATABASE"));
    }
}
