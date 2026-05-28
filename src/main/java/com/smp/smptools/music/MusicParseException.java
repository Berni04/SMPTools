package com.smp.smptools.music;

/**
 * Exception thrown when an error occurs while parsing NBS (Note Block Studio) files.
 * This exception indicates that the NBS file format is invalid or corrupted.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 * @see NBSParser
 */
public class MusicParseException extends Exception {

    /**
     * Constructs a new MusicParseException with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public MusicParseException(String message) {
        super(message);
    }

    /**
     * Constructs a new MusicParseException with the specified detail message and cause.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public MusicParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
