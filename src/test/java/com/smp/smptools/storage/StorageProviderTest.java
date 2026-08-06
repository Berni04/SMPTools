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
}
