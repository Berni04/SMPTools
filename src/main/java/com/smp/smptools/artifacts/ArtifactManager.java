package com.smp.smptools.artifacts;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages creation, verification, and persistence of Custom Utility Artifacts.
 */
public class ArtifactManager {

    private final SMPTools plugin;
    private final NamespacedKey artifactKey;
    private final Map<UUID, Map<Integer, ItemStack>> equippedPouchMap = new HashMap<>();
    private final Map<UUID, List<ItemStack>> pouchOverflow = new HashMap<>();
    private File artifactsFile;
    private FileConfiguration artifactsConfig;

    public ArtifactManager(SMPTools plugin) {
        this.plugin = plugin;
        this.artifactKey = plugin != null ? new NamespacedKey(plugin, "artifact_type") : new NamespacedKey("smptools", "artifact_type");
        loadPouchData();
    }

    public ItemStack createArtifact(ArtifactType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<!italic><gold><b>" + type.getFormattedName() + "</b></gold>"));
            
            List<Component> lore = new ArrayList<>();
            lore.add(MiniMessage.miniMessage().deserialize("<!italic><gray>" + type.getDescription() + "</gray>"));
            lore.add(Component.empty());
            lore.add(MiniMessage.miniMessage().deserialize("<!italic><yellow>Type: " + (type.getSlotType() == ArtifactType.ArtifactSlotType.PASSIVE ? "Passive (/artifacts)" : "Active Item") + "</yellow>"));
            lore.add(MiniMessage.miniMessage().deserialize("<!italic><purple>✨ Custom SMPTools Artifact</purple>"));
            
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(true);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(artifactKey, PersistentDataType.STRING, type.name());

            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isArtifact(ItemStack item, ArtifactType type) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(artifactKey, PersistentDataType.STRING)) {
            String val = pdc.get(artifactKey, PersistentDataType.STRING);
            return type.name().equalsIgnoreCase(val);
        }
        return false;
    }

    public ArtifactType getArtifactType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(artifactKey, PersistentDataType.STRING)) {
            String val = pdc.get(artifactKey, PersistentDataType.STRING);
            try {
                return ArtifactType.valueOf(val);
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    public boolean hasEquippedArtifact(Player player, ArtifactType type) {
        if (player == null) return false;
        Map<Integer, ItemStack> pouch = getEquippedPouch(player.getUniqueId());
        for (ItemStack item : pouch.values()) {
            if (isArtifact(item, type)) {
                return true;
            }
        }
        return false;
    }

    public Map<Integer, ItemStack> getEquippedPouch(UUID uuid) {
        return equippedPouchMap.computeIfAbsent(uuid, k -> new HashMap<>());
    }


    public static final Set<Integer> ALLOWED_POUCH_SLOTS = Set.of(10, 11, 12, 13, 14, 15, 16);

    public static boolean isAllowedPouchSlot(int slot) {
        return ALLOWED_POUCH_SLOTS.contains(slot);
    }

    public void setEquippedPouch(UUID uuid, Map<Integer, ItemStack> slots) {
        Map<Integer, ItemStack> pouch = getEquippedPouch(uuid);
        pouch.clear();
        if (slots != null) {
            for (Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
                if (entry.getKey() == null || !isAllowedPouchSlot(entry.getKey())) {
                    continue;
                }
                ItemStack item = entry.getValue();
                if (item != null && !item.getType().isAir()) {
                    ArtifactType type = getArtifactType(item);
                    if (type != null && type.getSlotType() == ArtifactType.ArtifactSlotType.PASSIVE) {
                        pouch.put(entry.getKey(), item);
                    }
                }
            }
        }
        savePouchData();
    }

    private void loadPouchData() {
        if (plugin == null) return;
        artifactsFile = new File(plugin.getDataFolder(), "artifacts.yml");
        if (!artifactsFile.exists()) {
            try {
                artifactsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not create artifacts.yml", e);
            }
        }
        artifactsConfig = YamlConfiguration.loadConfiguration(artifactsFile);

        pouchOverflow.clear();
        if (artifactsConfig.isConfigurationSection("pouch-overflow")) {
            org.bukkit.configuration.ConfigurationSection overflowSec = artifactsConfig.getConfigurationSection("pouch-overflow");
            if (overflowSec != null) {
                for (String uuidStr : overflowSec.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        List<?> rawList = artifactsConfig.getList("pouch-overflow." + uuidStr);
                        if (rawList != null) {
                            List<ItemStack> items = new ArrayList<>();
                            for (Object obj : rawList) {
                                if (obj instanceof ItemStack is) items.add(is);
                            }
                            if (!items.isEmpty()) {
                                pouchOverflow.put(uuid, items);
                            }
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Failed to load pouch overflow for UUID '" + uuidStr + "': " + ex.getMessage());
                    }
                }
            }
        }

        if (artifactsConfig.isConfigurationSection("pouch")) {
            org.bukkit.configuration.ConfigurationSection pouchSec = artifactsConfig.getConfigurationSection("pouch");
            if (pouchSec != null) {
                for (String uuidStr : pouchSec.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        Map<Integer, ItemStack> map = new HashMap<>();
                        org.bukkit.configuration.ConfigurationSection userSec = artifactsConfig.getConfigurationSection("pouch." + uuidStr);
                        if (userSec != null) {
                            List<Map.Entry<Integer, ItemStack>> legacyEntries = new ArrayList<>();
                            for (String slotStr : userSec.getKeys(false)) {
                                try {
                                    int slot = Integer.parseInt(slotStr);
                                    ItemStack item = artifactsConfig.getItemStack("pouch." + uuidStr + "." + slotStr);
                                    if (item != null) {
                                        ArtifactType type = getArtifactType(item);
                                        if (type != null && type.getSlotType() == ArtifactType.ArtifactSlotType.PASSIVE) {
                                            if (isAllowedPouchSlot(slot)) {
                                                map.put(slot, item);
                                            } else {
                                                legacyEntries.add(Map.entry(slot, item));
                                            }
                                        }
                                    }
                                } catch (Exception ex) {
                                    plugin.getLogger().warning("Failed to load pouch slot '" + slotStr + "' for " + uuidStr + ": " + ex.getMessage());
                                }
                            }
                            for (Map.Entry<Integer, ItemStack> legacy : legacyEntries) {
                                int legacySlot = legacy.getKey();
                                ItemStack item = legacy.getValue();
                                Integer freeSlot = null;
                                for (int allowed : ALLOWED_POUCH_SLOTS.stream().sorted().toList()) {
                                    if (!map.containsKey(allowed)) {
                                        freeSlot = allowed;
                                        break;
                                    }
                                }
                                if (freeSlot != null) {
                                    map.put(freeSlot, item);
                                    if (plugin != null) {
                                        plugin.getLogger().info("Migrated pouch artifact for " + uuidStr + " from legacy slot " + legacySlot + " to slot " + freeSlot);
                                    }
                                } else {
                                    pouchOverflow.computeIfAbsent(uuid, k -> new ArrayList<>()).add(item);
                                    if (plugin != null) {
                                        plugin.getLogger().warning("No free pouch slot for " + uuidStr + " to migrate legacy slot " + legacySlot + "; preserved in recovery overflow storage.");
                                    }
                                }
                            }
                        }
                        equippedPouchMap.put(uuid, map);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Failed to load pouch data for UUID '" + uuidStr + "': " + ex.getMessage());
                    }
                }
            }
        }
    }

    public boolean hasPouchOverflow(UUID uuid) {
        if (uuid == null) return false;
        List<ItemStack> list = pouchOverflow.get(uuid);
        return list != null && !list.isEmpty();
    }

    public List<ItemStack> getPouchOverflow(UUID uuid) {
        if (uuid == null) return Collections.emptyList();
        List<ItemStack> list = pouchOverflow.get(uuid);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    /**
     * Claims and delivers any overflow artifacts to the player.
     * Delivered items are removed from pouchOverflow and persisted.
     * If persistence fails, state is rolled back, no items are delivered, and null is returned.
     *
     * @param player the player receiving the overflow items
     * @return the list of delivered ItemStacks, or null if persistent storage failed
     */
    public List<ItemStack> claimPouchOverflow(Player player) {
        if (player == null) return Collections.emptyList();
        UUID uuid = player.getUniqueId();
        List<ItemStack> list = pouchOverflow.get(uuid);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<ItemStack> toDeliver = new ArrayList<>(list);

        // Stage removal and verify persistence before delivering items
        pouchOverflow.remove(uuid);
        if (!savePouchDataWithResult()) {
            // Restore in-memory state on persistence failure and abort claim
            pouchOverflow.put(uuid, list);
            return null;
        }

        List<ItemStack> delivered = new ArrayList<>();
        List<ItemStack> undelivered = new ArrayList<>();

        for (ItemStack item : toDeliver) {
            if (item == null || item.getType() == Material.AIR) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            if (leftover.isEmpty()) {
                delivered.add(item);
            } else {
                boolean droppedAll = true;
                for (ItemStack drop : leftover.values()) {
                    if (drop != null && drop.getType() != Material.AIR) {
                        if (player.getWorld() != null) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        } else {
                            undelivered.add(drop);
                            droppedAll = false;
                        }
                    }
                }
                if (droppedAll) {
                    delivered.add(item);
                }
            }
        }

        if (!undelivered.isEmpty()) {
            pouchOverflow.computeIfAbsent(uuid, k -> new ArrayList<>()).addAll(undelivered);
            savePouchDataWithResult();
        }

        return delivered;
    }

    public void savePouchData() {
        savePouchDataWithResult();
    }

    public boolean savePouchDataWithResult() {
        if (artifactsConfig == null || artifactsFile == null) return true;
        artifactsConfig.set("pouch", null);

        for (Map.Entry<UUID, Map<Integer, ItemStack>> entry : equippedPouchMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<Integer, ItemStack> slotEntry : entry.getValue().entrySet()) {
                if (slotEntry != null && slotEntry.getKey() != null && isAllowedPouchSlot(slotEntry.getKey())) {
                    artifactsConfig.set("pouch." + uuidStr + "." + slotEntry.getKey(), slotEntry.getValue());
                }
            }
        }

        artifactsConfig.set("pouch-overflow", null);
        for (Map.Entry<UUID, List<ItemStack>> entry : pouchOverflow.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                artifactsConfig.set("pouch-overflow." + entry.getKey(), entry.getValue());
            }
        }

        try {
            com.smp.smptools.utils.AtomicFileWriter.save(artifactsConfig, artifactsFile);
            return true;
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save artifacts.yml", e);
            }
            return false;
        }
    }
}
