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
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tmp = parent != null
                ? parent.resolve(target.getFileName() + "." + UUID.randomUUID() + ".tmp")
                : Files.createTempFile("smptools-atomic-", ".tmp");

        try {
            Files.write(tmp, data,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.SYNC);

            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveEx) {
                try {
                    Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception ignored) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
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
