package com.smp.smptools.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeathMessageListenerTest {

    @Test
    public void testReplaceKillerPlaceholderAtEnd() throws Exception {
        DeathMessageListener listener = new DeathMessageListener(null);
        Method method = DeathMessageListener.class.getDeclaredMethod("replaceKillerPlaceholder", Component.class, String.class, Component.class);
        method.setAccessible(true);

        Component player = Component.text("PlayerA");
        Component killer = Component.text("PlayerB");
        String template = " was sent back to the lobby by %killer%.";

        Component result = (Component) method.invoke(listener, player, template, killer);
        String plainText = PlainTextComponentSerializer.plainText().serialize(result);

        assertEquals("PlayerA was sent back to the lobby by PlayerB.", plainText);
    }

    @Test
    public void testReplaceKillerPlaceholderInMiddle() throws Exception {
        DeathMessageListener listener = new DeathMessageListener(null);
        Method method = DeathMessageListener.class.getDeclaredMethod("replaceKillerPlaceholder", Component.class, String.class, Component.class);
        method.setAccessible(true);

        Component player = Component.text("PlayerA");
        Component killer = Component.text("PlayerB");
        String template = " learned that %killer% is not their friend.";

        Component result = (Component) method.invoke(listener, player, template, killer);
        String plainText = PlainTextComponentSerializer.plainText().serialize(result);

        assertEquals("PlayerA learned that PlayerB is not their friend.", plainText);
    }

    @Test
    public void testReplaceKillerPlaceholderWithoutPlaceholder() throws Exception {
        DeathMessageListener listener = new DeathMessageListener(null);
        Method method = DeathMessageListener.class.getDeclaredMethod("replaceKillerPlaceholder", Component.class, String.class, Component.class);
        method.setAccessible(true);

        Component player = Component.text("PlayerA");
        Component killer = Component.text("PlayerB");
        String template = " was outplayed by ";

        Component result = (Component) method.invoke(listener, player, template, killer);
        String plainText = PlainTextComponentSerializer.plainText().serialize(result);

        assertEquals("PlayerA was outplayed by PlayerB", plainText);
    }
}
