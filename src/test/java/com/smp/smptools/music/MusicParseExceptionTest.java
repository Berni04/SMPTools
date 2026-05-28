package com.smp.smptools.music;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MusicParseExceptionTest {

    @Test
    void constructor_withMessage_setsMessage() {
        MusicParseException exception = new MusicParseException("Test error");
        assertEquals("Test error", exception.getMessage());
    }

    @Test
    void constructor_withMessageAndCause_setsBoth() {
        Exception cause = new IOException("IO error");
        MusicParseException exception = new MusicParseException("Parse error", cause);
        assertEquals("Parse error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void isException() {
        MusicParseException exception = new MusicParseException("Test");
        assertInstanceOf(Exception.class, exception);
    }
}
