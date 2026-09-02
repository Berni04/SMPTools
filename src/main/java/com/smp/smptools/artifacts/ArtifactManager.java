package com.smp.smptools.artifacts;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private File artifactsFile;
    private FileConfiguration artifactsConfig;

    public ArtifactManager(SMPTools plugin) {
        this.plugin = plugin;
        this.artifactKey = new NamespacedKey(plugin, "artifact_type");
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


    public void setEquippedPouch(UUID uuid, Map<Integer, ItemStack> slots) {
        Map<Integer, ItemStack> pouch = getEquippedPouch(uuid);
        pouch.clear();
        if (slots != null) {
            for (Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
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
        artifactsFile = new File(plugin.getDataFolder(), "artifacts.yml");
        if (!artifactsFile.exists()) {
            try {
                artifactsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not create artifacts.yml", e);
            }
        }
        artifactsConfig = YamlConfiguration.loadConfiguration(artifactsFile);

        if (artifactsConfig.isConfigurationSection("pouch")) {
            org.bukkit.configuration.ConfigurationSection pouchSec = artifactsConfig.getConfigurationSection("pouch");
            if (pouchSec != null) {
                for (String uuidStr : pouchSec.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        Map<Integer, ItemStack> map = new HashMap<>();
                        org.bukkit.configuration.ConfigurationSection userSec = artifactsConfig.getConfigurationSection("pouch." + uuidStr);
                        if (userSec != null) {
                            for (String slotStr : userSec.getKeys(false)) {
                                try {
                                    int slot = Integer.parseInt(slotStr);
                                    if (slot < 0 || slot >= 54) {
                                        continue;
                                    }
                                    ItemStack item = artifactsConfig.getItemStack("pouch." + uuidStr + "." + slotStr);
                                    if (item != null) {
                                        ArtifactType type = getArtifactType(item);
                                        if (type != null && type.getSlotType() == ArtifactType.ArtifactSlotType.PASSIVE) {
                                            map.put(slot, item);
                                        }
                                    }
                                } catch (Exception ex) {
                                    plugin.getLogger().warning("Failed to load pouch slot '" + slotStr + "' for " + uuidStr + ": " + ex.getMessage());
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

    public void savePouchData() {
        if (artifactsConfig == null || artifactsFile == null) return;
        artifactsConfig.set("pouch", null);

        for (Map.Entry<UUID, Map<Integer, ItemStack>> entry : equippedPouchMap.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<Integer, ItemStack> slotEntry : entry.getValue().entrySet()) {
                if (slotEntry.getKey() >= 0 && slotEntry.getKey() < 54) {
                    artifactsConfig.set("pouch." + uuidStr + "." + slotEntry.getKey(), slotEntry.getValue());
                }
            }
        }

        try {
            com.smp.smptools.utils.AtomicFileWriter.save(artifactsConfig, artifactsFile);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save artifacts.yml", e);
        }
    }
}
