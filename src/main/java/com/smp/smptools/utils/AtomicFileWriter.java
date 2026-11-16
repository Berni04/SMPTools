package com.smp.smptools.utils;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Utility for performing atomic file writes using temp file + atomic move.
 * Prevents corrupted or truncated files in the event of JVM crash, OOM, or sudden kill.
 */
public final class AtomicFileWriter {

    private AtomicFileWriter() {
        // Prevent instantiation
    }

    /**
     * Writes raw bytes to the target path atomically.
     *
     * @param target the target file path
     * @param data   the byte array to write
     * @throws IOException if an I/O error occurs
     */
    public static void write(Path target, byte[] data) throws IOException {
        Path absTarget = target.toAbsolutePath();
        Path parent = absTarget.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tmp = parent != null
                ? parent.resolve(absTarget.getFileName() + "." + UUID.randomUUID() + ".tmp")
                : Files.createTempFile(absTarget.getFileName().toString(), ".tmp");

        try {
            Files.write(tmp, data,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.SYNC);

            Files.move(tmp, absTarget, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /**
     * Serializes a Bukkit {@link FileConfiguration} to string and writes it atomically to disk.
     *
     * @param config the FileConfiguration to save
     * @param file   the target file
     * @throws IOException if an I/O error occurs
     */
    public static void save(FileConfiguration config, File file) throws IOException {
        if (config == null || file == null) {
            throw new IllegalArgumentException("config and file must not be null");
        }
        write(file.toPath(), config.saveToString().getBytes(StandardCharsets.UTF_8));
    }
}
