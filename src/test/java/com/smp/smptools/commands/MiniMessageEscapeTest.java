package com.smp.smptools.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for the {@code <player>}/<message>} angle-bracket stripping
 * bug in {@code /msg} usage text.
 *
 * <p>Before the fix, the usage string {@code "/msg <player> <message>"} was
 * fed directly to {@code MiniMessage.deserialize(...)} which silently
 * interpreted {@code <player>} and {@code <message>} as tags and dropped
 * them, leaving the player with an empty usage message.</p>
 */
class MiniMessageEscapeTest {

    @Test
    void escape_preservesAngleBrackets() {
        String escaped = MiniMessage.escape("/msg <player> <message>");
        // After escape, < and > should be preserved as literal characters
        // so the rendered Component contains "msg <player> <message>".
        assertTrue(escaped.contains("<player>"),
                "MiniMessage.escape should preserve <player> as literal text. Got: " + escaped);
        assertTrue(escaped.contains("<message>"),
                "MiniMessage.escape should preserve <message> as literal text. Got: " + escaped);
    }

    @Test
    void escape_thenDeserialize_keepsContent() {
        String escaped = MiniMessage.escape("/msg <player> <message>");
        Component rendered = MiniMessage.miniMessage().deserialize(escaped);
        String plain = PlainTextComponentSerializer.plainText().serialize(rendered);
        assertEquals("/msg <player> <message>", plain,
                "After escape + deserialize, the angle brackets should still be present in the output");
    }

    @Test
    void withoutEscape_unknownTagThrows() {
        // Demonstrates the original bug: without escaping, MiniMessage throws
        // a ParseException on the unknown <player> tag because the default
        // strict parser does not recognize it.
        assertThrows(Exception.class,
                () -> MiniMessage.miniMessage().deserialize(
                        "<gray>Usage: /msg <player> <message></gray>"),
                "Without escape, MiniMessage should throw on the unknown <player> tag");
    }
}
