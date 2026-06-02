package com.smp.smptools.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MessageManager} and the externalized {@code messages.yml}.
 *
 * <p>Loads the shipped {@code messages.yml} from the test classpath and
 * validates that the templates used by the TPA flow reference the
 * {@code <requester_name_color>} tag (so that the requester's colored name
 * is rendered to the recipient, not the recipient's own name).</p>
 */
class MessageManagerTest {

    @Test
    void tpaRequestReceived_usesRequesterNameColorTag() throws IOException {
        FileConfiguration cfg = loadShippedMessages();
        String template = cfg.getString("tpa.request-received");
        assertNotNull(template, "tpa.request-received template missing from messages.yml");
        assertTrue(template.contains("<requester_name_color>"),
                "tpa.request-received must reference <requester_name_color> to show the requester's "
                        + "colored name. Got: " + template);
        assertFalse(template.contains("<player_name_color>"),
                "tpa.request-received must NOT use <player_name_color> (would show recipient's name). "
                        + "Got: " + template);
    }

    @Test
    void tpaAccepted_usesRequesterNameColorTag() throws IOException {
        FileConfiguration cfg = loadShippedMessages();
        String template = cfg.getString("tpa.accepted");
        assertNotNull(template, "tpa.accepted template missing from messages.yml");
        assertTrue(template.contains("<requester_name_color>"),
                "tpa.accepted must reference <requester_name_color> to show the requester's "
                        + "colored name. Got: " + template);
        assertFalse(template.contains("<player_name_color>"),
                "tpa.accepted must NOT use <player_name_color> (would show acceptor's name). "
                        + "Got: " + template);
    }

    @Test
    void pingPlayerPing_usesPlaceholderNotTag() throws IOException {
        FileConfiguration cfg = loadShippedMessages();
        String template = cfg.getString("ping.player-ping");
        assertNotNull(template, "ping.player-ping template missing from messages.yml");
        assertTrue(template.contains("{player}"),
                "ping.player-ping should use {player} placeholder for console compatibility. "
                        + "Got: " + template);
    }

    @Test
    void commonKeys_present() throws IOException {
        FileConfiguration cfg = loadShippedMessages();
        assertNotNull(cfg.getString("common.player-only"));
        assertNotNull(cfg.getString("common.no-permission"));
        assertNotNull(cfg.getString("common.usage"));
    }

    /**
     * Loads the shipped {@code messages.yml} from src/main/resources into a
     * temporary file and returns the parsed configuration.
     */
    private FileConfiguration loadShippedMessages() throws IOException {
        File temp = File.createTempFile("messages", ".yml");
        temp.deleteOnExit();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("messages.yml")) {
            assertNotNull(in, "messages.yml not found on test classpath");
            Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return YamlConfiguration.loadConfiguration(temp);
    }
}
