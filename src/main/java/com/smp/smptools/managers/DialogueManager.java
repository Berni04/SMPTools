package com.smp.smptools.managers;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DialogueManager implements Listener {

    private final SMPTools plugin;
    private FileConfiguration dialogueConfig;
    private final Map<UUID, String> playerCurrentDialogue = new ConcurrentHashMap<>(); // Player UUID -> Dialogue ID
    private final Map<UUID, String> playerCurrentLine = new ConcurrentHashMap<>(); // Player UUID -> Line ID
    private final Map<UUID, UUID> activeTextDisplays = new ConcurrentHashMap<>(); // Player UUID -> TextDisplay Entity UUID
    private final Map<UUID, UUID> playerNPC = new ConcurrentHashMap<>(); // Player UUID -> NPC Entity UUID
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> scheduledCallbacks = new ConcurrentHashMap<>();

    public DialogueManager(SMPTools plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadDialogues();
    }

    public void loadDialogues() {
        File file = new File(plugin.getDataFolder(), "dialogues.yml");
        if (!file.exists()) {
            plugin.saveResource("dialogues.yml", true);
        }
        dialogueConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void startDialogue(Player player, Entity npc, String dialogueId) {
        if (dialogueConfig.getConfigurationSection("dialogues." + dialogueId) == null) {
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("dialogue.no-dialogue"));
            return;
        }

        // Clear previous dialogue if any
        stopDialogue(player);

        playerCurrentDialogue.put(player.getUniqueId(), dialogueId);
        playerNPC.put(player.getUniqueId(), npc.getUniqueId());
        showLine(player, npc, "start");
    }

    public void advanceDialogue(Player player, Entity npc) {
        String dialogueId = playerCurrentDialogue.get(player.getUniqueId());
        String currentLineId = playerCurrentLine.get(player.getUniqueId());

        if (dialogueId == null || currentLineId == null) {
            return; // No active dialogue
        }

        String path = "dialogues." + dialogueId + "." + currentLineId;
        String nextLine = dialogueConfig.getString(path + ".next");

        if (nextLine != null && !nextLine.equalsIgnoreCase("end")) {
            showLine(player, npc, nextLine);
        } else {
            stopDialogue(player);
        }
    }

    private void showLine(Player player, Entity npc, String lineId) {
        playerCurrentLine.put(player.getUniqueId(), lineId);
        String dialogueId = playerCurrentDialogue.get(player.getUniqueId());
        String path = "dialogues." + dialogueId + "." + lineId;

        String text = dialogueConfig.getString(path + ".text");
        if (text == null)
            return;

        // Remove old display
        removeTextDisplay(player);

        // Spawn new TextDisplay above NPC
        Location displayLoc = npc.getLocation().add(0, npc.getHeight() + 0.9, 0);
        TextDisplay display = (TextDisplay) npc.getWorld().spawnEntity(displayLoc, EntityType.TEXT_DISPLAY);

        display.text(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
        display.setBillboard(Display.Billboard.VERTICAL); // Always face player
        display.setBackgroundColor(org.bukkit.Color.fromARGB(100, 0, 0, 0)); // Semi-transparent background
        display.setSeeThrough(false);
        display.setShadowed(true);

        // Make it only visible to the specific player
        display.setVisibleByDefault(false);
        player.showEntity(plugin, display);

        activeTextDisplays.put(player.getUniqueId(), display.getUniqueId());

        // Handle options if any
        if (dialogueConfig.contains(path + ".options")) {
            java.util.List<java.util.Map<?, ?>> options = dialogueConfig.getMapList(path + ".options");
            player.sendMessage(Component.empty());
            for (int i = 0; i < options.size(); i++) {
                java.util.Map<?, ?> option = options.get(i);
                String optionText = (String) option.get("text");
                Component optionComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(optionText)
                        .clickEvent(ClickEvent.runCommand("/npc respond " + dialogueId + " " + lineId + " " + i))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent
                                .showText(Component.text("Click to select", NamedTextColor.GRAY)));
                player.sendMessage(optionComponent);
            }
            player.sendMessage(Component.empty());
        }

        // Auto-despawn after some time if player walks away
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                if (!display.isValid()) {
                    this.cancel();
                    return;
                }
                if (player.getLocation().distanceSquared(display.getLocation()) > 100) {
                    removeTextDisplay(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void handleOptionSelection(Player player, String dialogueId, String lineId, int optionIndex) {
        if (!playerCurrentDialogue.containsKey(player.getUniqueId())
                || !playerCurrentDialogue.get(player.getUniqueId()).equals(dialogueId)) {
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("dialogue.expired"));
            return;
        }

        String path = "dialogues." + dialogueId + "." + lineId + ".options";
        if (!dialogueConfig.contains(path)) {
            return;
        }

        java.util.List<java.util.Map<?, ?>> options = dialogueConfig.getMapList(path);
        if (optionIndex < 0 || optionIndex >= options.size()) {
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("dialogue.invalid-option"));
            return;
        }

        java.util.Map<?, ?> option = options.get(optionIndex);
        String nextLine = (String) option.get("next");
        String command = (String) option.get("command");

        if (command != null) {
            player.performCommand(command);
        }

        // Remove the text display immediately upon selection
        removeTextDisplay(player);

        // Show the player's selected option above their head
        String optionText = (String) option.get("text");
        showPlayerText(player, optionText);

        if (nextLine != null && !nextLine.equalsIgnoreCase("end")) {
            // Delay the next NPC line to allow reading the player's text
            org.bukkit.scheduler.BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    scheduledCallbacks.remove(player.getUniqueId());
                    UUID npcUUID = playerNPC.get(player.getUniqueId());
                    if (npcUUID != null) {
                        Entity npc = Bukkit.getEntity(npcUUID);
                        if (npc != null && npc.isValid()) {
                            showLine(player, npc, nextLine);
                        } else {
                            stopDialogue(player);
                        }
                    } else {
                        stopDialogue(player);
                    }
                }
            }.runTaskLater(plugin, 60L); // 3 seconds delay
            org.bukkit.scheduler.BukkitTask old = scheduledCallbacks.put(player.getUniqueId(), task);
            if (old != null) {
                try { old.cancel(); } catch (Exception ignored) {}
            }
        } else {
            // End of dialogue reached
            org.bukkit.scheduler.BukkitTask endTask = new BukkitRunnable() {
                @Override
                public void run() {
                    scheduledCallbacks.remove(player.getUniqueId());
                    stopDialogue(player);
                }
            }.runTaskLater(plugin, 60L);
            org.bukkit.scheduler.BukkitTask old = scheduledCallbacks.put(player.getUniqueId(), endTask);
            if (old != null) {
                try { old.cancel(); } catch (Exception ignored) {}
            }
        }
    }

    private void showPlayerText(Player player, String text) {
        if (player == null || !player.isOnline()) return;

        // Spawn a TextDisplay right above the player's head
        Location loc = player.getLocation().add(0, 2.2, 0);
        TextDisplay display = (TextDisplay) player.getWorld().spawn(loc, TextDisplay.class);

        display.text(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
        display.setBillboard(Display.Billboard.CENTER);
        display.setDefaultBackground(true);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setVisibleByDefault(false);
        player.showEntity(plugin, display);

        activeTextDisplays.put(player.getUniqueId(), display.getUniqueId());

        // Mount it on the player
        player.addPassenger(display);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopDialogue(event.getPlayer());
    }

    public void stopDialogue(Player player) {
        if (player == null) return;
        org.bukkit.scheduler.BukkitTask task = scheduledCallbacks.remove(player.getUniqueId());
        if (task != null) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
        playerCurrentDialogue.remove(player.getUniqueId());
        playerCurrentLine.remove(player.getUniqueId());
        playerNPC.remove(player.getUniqueId());
        removeTextDisplay(player);
    }

    private void removeTextDisplay(Player player) {
        if (player == null) return;
        UUID displayUUID = activeTextDisplays.remove(player.getUniqueId());
        if (displayUUID != null) {
            Entity entity = Bukkit.getEntity(displayUUID);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    public void cleanupAll() {
        for (org.bukkit.scheduler.BukkitTask task : scheduledCallbacks.values()) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
        scheduledCallbacks.clear();
        for (UUID displayUUID : activeTextDisplays.values()) {
            Entity entity = Bukkit.getEntity(displayUUID);
            if (entity != null) {
                entity.remove();
            }
        }
        activeTextDisplays.clear();
        playerCurrentDialogue.clear();
        playerCurrentLine.clear();
        playerNPC.clear();
    }

    public boolean isInDialogue(Player player) {
        return player != null && playerCurrentDialogue.containsKey(player.getUniqueId());
    }
}
