package com.smp.smptools.storage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StorageProviderTest {

    @Test
    public void testParseCanonicalValueBooleanValid() {
        assertEquals(Boolean.TRUE, StorageProvider.parseCanonicalValue("true", true));
        assertEquals(Boolean.TRUE, StorageProvider.parseCanonicalValue("TRUE", true));
        assertEquals(Boolean.TRUE, StorageProvider.parseCanonicalValue("True", false));
        assertEquals(Boolean.FALSE, StorageProvider.parseCanonicalValue("false", true));
        assertEquals(Boolean.FALSE, StorageProvider.parseCanonicalValue("FALSE", true));
        assertEquals(Boolean.FALSE, StorageProvider.parseCanonicalValue("False", true));
    }

    @Test
    public void testParseCanonicalValueBooleanInvalidReturnsDefaultValue() {
        assertEquals(true, StorageProvider.parseCanonicalValue("invalid", true));
        assertEquals(false, StorageProvider.parseCanonicalValue("invalid", false));
        assertEquals(true, StorageProvider.parseCanonicalValue("123", true));
        assertEquals(false, StorageProvider.parseCanonicalValue("yes", false));
        assertEquals(true, StorageProvider.parseCanonicalValue("0", true));
    }

    @Test
    public void testParseCanonicalValueBooleanRawBoolean() {
        assertEquals(true, StorageProvider.parseCanonicalValue(true, false));
        assertEquals(false, StorageProvider.parseCanonicalValue(false, true));
    }

    @Test
    public void testParseCanonicalValueDecimalToLong() {
        assertEquals(1L, StorageProvider.parseCanonicalValue(Double.valueOf(1.0), 0L));
        assertEquals(1L, StorageProvider.parseCanonicalValue(Float.valueOf(1.0f), 0L));
        assertEquals(42L, StorageProvider.parseCanonicalValue(Double.valueOf(42.5), 0L));
        assertEquals(0L, StorageProvider.parseCanonicalValue("Infinity", 0L));
        assertEquals(0L, StorageProvider.parseCanonicalValue("-Infinity", 0L));
        assertEquals(0L, StorageProvider.parseCanonicalValue("NaN", 0L));
    }
}
