package com.smp.smptools.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for the {@code <player>}/<message>} angle-bracket stripping
 * bug in {@code /msg} usage text.
 *
 * <p>Before the fix, the usage string {@code "/msg <player> <message>"} was
 * fed directly to {@code MiniMessage.deserialize(...)} which interpreted
 * {@code <player>} and {@code <message>} as unknown tags and dropped them
 * (or threw a {@code ParseException} depending on the version), leaving
 * the player with a broken usage message.</p>
 *
 * <p>MiniMessage 4.25.0 does not provide an {@code escape(...)} method, and
 * backslash-escaping of {@code \>} is not recognised. The robust fix is to
 * build a MiniMessage instance with {@link TagResolver#empty()} so that
 * every {@code <...>} sequence is treated as literal text.</p>
 */
class MiniMessageEscapeTest {

    private static final MiniMessage LITERAL = MiniMessage.builder()
            .tags(TagResolver.empty())
            .build();

    @Test
    void literalMiniMessage_preservesAngleBrackets() {
        Component rendered = LITERAL.deserialize("/msg <player> <message>");
        String plain = PlainTextComponentSerializer.plainText().serialize(rendered);
        assertEquals("/msg <player> <message>", plain,
                "TagResolver.empty() MiniMessage should treat <player> as literal text. Got: " + plain);
    }

    @Test
    void literalMiniMessage_preservesOnlyOpenTag() {
        // Demonstrates that a malformed-looking usage like <foo without
        // closing > also survives without throwing.
        Component rendered = LITERAL.deserialize("/msg <player no close");
        String plain = PlainTextComponentSerializer.plainText().serialize(rendered);
        assertEquals("/msg <player no close", plain,
                "Literal MiniMessage should not throw on unclosed tags. Got: " + plain);
    }

    @Test
    void defaultMiniMessage_keepsUnknownTagsAsText() {
        // Documents the actual behavior of MiniMessage 4.25.0: unknown tags
        // like <player> are kept as literal text rather than being stripped
        // or causing a ParseException. This means the original /msg bug
        // (where <player> and <message> disappeared from the usage) was
        // never observable in the default parser, but the explicit
        // TagResolver.empty() approach is still preferred because it makes
        // the intent clear and prevents future regressions if a custom tag
        // resolver is ever registered.
        Component rendered = MiniMessage.miniMessage().deserialize(
                "<gray>Usage: /msg <player> <message></gray>");
        String plain = PlainTextComponentSerializer.plainText().serialize(rendered);
        assertTrue(plain.contains("<player>"),
                "Default MiniMessage keeps <player> as literal text. Got: " + plain);
        assertTrue(plain.contains("<message>"),
                "Default MiniMessage keeps <message> as literal text. Got: " + plain);
    }

    @Test
    void literalMiniMessage_doesNotApplyColors() {
        // Sanity check: with TagResolver.empty(), even a known color tag
        // like <red> is left as literal text (no color applied).
        Component rendered = LITERAL.deserialize("<red>hello</red>");
        String plain = PlainTextComponentSerializer.plainText().serialize(rendered);
        assertEquals("<red>hello</red>", plain,
                "TagResolver.empty() should not interpret <red> as a color tag. Got: " + plain);
    }
}
