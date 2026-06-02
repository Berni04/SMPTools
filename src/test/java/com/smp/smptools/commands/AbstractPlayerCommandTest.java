package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.config.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for the console-routing in {@link AbstractPlayerCommand}.
 *
 * <p>Verifies that:</p>
 * <ul>
 *   <li>Commands that don't override {@code allowConsole()} reject console senders.</li>
 *   <li>Commands that override {@code allowConsole()} to return {@code true}
 *       invoke {@code onConsoleCommand(...)} for non-player senders.</li>
 * </ul>
 *
 * <p>This test does not load the full plugin; it exercises the routing logic
 * through a minimal stub.</p>
 */
class AbstractPlayerCommandTest {

    @Test
    void playerOnlyCommand_rejectsConsoleSender() {
        TestCommand cmd = new TestCommand(false);
        StubConsoleSender console = new StubConsoleSender();
        boolean handled = cmd.onCommand(console, null, "test", new String[0]);
        assertTrue(handled, "Command should be handled (returning true), not ignored");
        assertEquals(1, console.messages.size(), "Console sender should receive the player-only message");
        String plain = PlainTextComponentSerializer.plainText().serialize(console.messages.get(0));
        assertTrue(plain.toLowerCase().contains("only players")
                        || plain.toLowerCase().contains("player"),
                "Expected player-only rejection, got: " + plain);
    }

    @Test
    void consoleFriendlyCommand_invokesConsoleHandler() {
        TestCommand cmd = new TestCommand(true);
        StubConsoleSender console = new StubConsoleSender();
        boolean handled = cmd.onCommand(console, null, "test", new String[0]);
        assertTrue(handled, "Command should be handled");
        assertEquals(0, console.messages.size(),
                "Console-friendly command should NOT send the player-only message");
        assertTrue(cmd.consoleInvoked, "onConsoleCommand should have been invoked");
    }

    /**
     * Minimal concrete command for testing the routing logic without
     * touching the real plugin graph.
     */
    private static final class TestCommand extends AbstractPlayerCommand {
        private final boolean consoleAllowed;
        boolean consoleInvoked = false;

        TestCommand(boolean consoleAllowed) {
            super(stubPlugin());
            this.consoleAllowed = consoleAllowed;
        }

        @Override
        protected boolean allowConsole() {
            return consoleAllowed;
        }

        @Override
        protected boolean onPlayerCommand(org.bukkit.entity.Player player, Command command, String label, String[] args) {
            return true;
        }

        @Override
        protected boolean onConsoleCommand(CommandSender sender, Command command, String label, String[] args) {
            consoleInvoked = true;
            return true;
        }
    }

    /**
     * Captures all messages sent to it for later assertion.
     */
    private static final class StubConsoleSender implements CommandSender {
        final java.util.List<Component> messages = new java.util.ArrayList<>();

        @Override
        public void sendMessage(Component message) {
            messages.add(message);
        }

        @Override
        public void sendMessage(String message) {
            messages.add(Component.text(message));
        }

        @Override
        public void sendMessage(String[] messages) {
            for (String m : messages) sendMessage(m);
        }

        @Override
        public void sendMessage(net.kyori.adventure.text.ComponentLike... messages) {
            for (net.kyori.adventure.text.ComponentLike m : messages) this.messages.add(m.asComponent());
        }

        @Override
        public void sendMessage(net.kyori.adventure.identity.Identity identity, Component... messages) {
            for (Component m : messages) this.messages.add(m);
        }

        @Override
        public void sendMessage(net.kyori.adventure.identity.Identity identity, net.kyori.adventure.text.ComponentLike... messages) {
            for (net.kyori.adventure.text.ComponentLike m : messages) this.messages.add(m.asComponent());
        }

        @Override
        public String getName() { return "CONSOLE"; }

        @Override
        public org.bukkit.Server getServer() { return null; }

        @Override
        public boolean isOp() { return true; }

        @Override
        public void setOp(boolean value) { }

        @Override
        public java.util.Set<org.bukkit.permissions.PermissionAttachmentInfo> getEffectivePermissions() { return Collections.emptySet(); }

        @Override
        public boolean hasPermission(String name) { return true; }

        @Override
        public boolean hasPermission(org.bukkit.permissions.Permission perm) { return true; }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin) { return null; }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value) { return null; }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value, int ticks) { return null; }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, int ticks) { return null; }

        @Override
        public void removeAttachment(org.bukkit.permissions.PermissionAttachment attachment) { }
    }

    /**
     * Returns a SMPTools instance with only a stubbed MessageManager. Other
     * accesses will NPE if the test exercises them, which is fine because
     * {@link AbstractPlayerCommand#onCommand} only calls
     * {@code plugin.getMessageManager()}.
     */
    private static SMPTools stubPlugin() {
        try {
            SMPTools plugin = (SMPTools) java.lang.reflect.Proxy.newProxyInstance(
                    SMPTools.class.getClassLoader(),
                    new Class<?>[]{SMPTools.class},
                    (proxy, method, args) -> {
                        if ("getMessageManager".equals(method.getName())) {
                            return stubMessageManager();
                        }
                        if ("toString".equals(method.getName())) {
                            return "StubSMPTools";
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                        return null;
                    });
            return plugin;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MessageManager stubMessageManager() {
        try {
            java.lang.reflect.Constructor<MessageManager> ctor =
                    MessageManager.class.getDeclaredConstructor(SMPTools.class);
            ctor.setAccessible(true);
            // We need a real instance; pass null and accept the NPE risk in
            // loadMessages (it only runs if files exist on disk).
            return ctor.newInstance((SMPTools) null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
