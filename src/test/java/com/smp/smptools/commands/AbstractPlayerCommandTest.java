package com.smp.smptools.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for the console-routing in {@link AbstractPlayerCommand}.
 *
 * <p>Verifies that a command overriding {@code allowConsole()} to return
 * {@code true} routes non-player senders to {@code onConsoleCommand(...)}
 * instead of replying with the player-only message.</p>
 *
 * <p>The player-only rejection path requires a working
 * {@link com.smp.smptools.config.MessageManager}, which in turn needs a
 * fully wired plugin instance. That is exercised separately by
 * {@code MessageManagerTest} and by integration testing on a live server.
 * Likewise, the player-routing branch relies on Java's {@code instanceof}
 * semantics, which need no test.</p>
 */
class AbstractPlayerCommandTest {

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

    @Test
    void consoleFriendlyCommand_passesArgsAndLabel() {
        TestCommand cmd = new TestCommand(true);
        StubConsoleSender console = new StubConsoleSender();
        String[] args = new String[]{"hello", "world"};
        boolean handled = cmd.onCommand(console, null, "test", args);
        assertTrue(handled);
        assertArrayEquals(args, cmd.lastArgs, "Args should be passed through to onConsoleCommand");
        assertEquals("test", cmd.lastLabel, "Label should be passed through to onConsoleCommand");
    }

    /**
     * Minimal concrete command for testing the routing logic. The plugin
     * reference is null because this test never exercises
     * {@code plugin.getMessageManager()}.
     */
    private static final class TestCommand extends AbstractPlayerCommand {
        private final boolean consoleAllowed;
        boolean consoleInvoked = false;
        String[] lastArgs;
        String lastLabel;

        TestCommand(boolean consoleAllowed) {
            super(null);
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
            lastArgs = args;
            lastLabel = label;
            return true;
        }
    }

    /**
     * Minimal {@link CommandSender} stub that captures all messages sent to it.
     * Only implements the methods actually exercised by {@link AbstractPlayerCommand}.
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
        public void sendMessage(String[] msgs) {
            for (String m : msgs) sendMessage(m);
        }

        @Override
        public void sendMessage(@org.jetbrains.annotations.Nullable java.util.UUID sender, @org.jetbrains.annotations.NotNull String... messages) {
            for (String m : messages) sendMessage(m);
        }

        @Override
        public void sendMessage(@org.jetbrains.annotations.Nullable java.util.UUID sender, @org.jetbrains.annotations.NotNull String message) {
            sendMessage(message);
        }

        @Override
        public Component name() {
            return Component.text("CONSOLE");
        }

        @Override
        public String getName() {
            return "CONSOLE";
        }

        @Override
        public CommandSender.Spigot spigot() {
            return new CommandSender.Spigot() {
            };
        }

        @Override
        public org.bukkit.Server getServer() {
            return null;
        }

        @Override
        public boolean isOp() {
            return true;
        }

        @Override
        public void setOp(boolean value) {
        }

        @Override
        public java.util.Set<org.bukkit.permissions.PermissionAttachmentInfo> getEffectivePermissions() {
            return Collections.emptySet();
        }

        @Override
        public boolean hasPermission(String name) {
            return true;
        }

        @Override
        public boolean hasPermission(org.bukkit.permissions.Permission perm) {
            return true;
        }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin) {
            return null;
        }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value) {
            return null;
        }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value, int ticks) {
            return null;
        }

        @Override
        public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, int ticks) {
            return null;
        }

        @Override
        public void removeAttachment(org.bukkit.permissions.PermissionAttachment attachment) {
        }

        @Override
        public void recalculatePermissions() {
        }
    }
}
