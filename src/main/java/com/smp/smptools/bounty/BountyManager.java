package com.smp.smptools.bounty;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BountyManager {

    private final SMPTools plugin;
    private final List<Bounty> bounties = new ArrayList<>();
    private File bountiesFile;
    private FileConfiguration bountiesConfig;

    public BountyManager(SMPTools plugin) {
        this.plugin = plugin;
        loadBounties();
    }

    public synchronized void loadBounties() {
        if (plugin == null) return;
        bounties.clear();

        bountiesFile = new File(plugin.getDataFolder(), "bounties.yml");
        if (!bountiesFile.exists()) {
            try {
                bountiesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create bounties.yml: " + e.getMessage());
            }
        }
        bountiesConfig = YamlConfiguration.loadConfiguration(bountiesFile);

        ConfigurationSection section = bountiesConfig.getConfigurationSection("bounties");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                try {
                    String placerStr = section.getString(id + ".placerUuid");
                    String placerName = section.getString(id + ".placerName", "Unknown");
                    String targetStr = section.getString(id + ".targetUuid");
                    String targetName = section.getString(id + ".targetName", "Unknown");
                    long placedTs = section.getLong(id + ".placedTimestamp", System.currentTimeMillis());
                    
                    String killerStr = section.getString(id + ".killerUuid");
                    long killedTs = section.getLong(id + ".killedTimestamp", 0L);
                    boolean claimed = section.getBoolean(id + ".claimed", false);

                    List<?> itemsRaw = section.getList(id + ".items");
                    List<ItemStack> items = new ArrayList<>();
                    if (itemsRaw != null) {
                        for (Object obj : itemsRaw) {
                            if (obj instanceof ItemStack is) {
                                items.add(is);
                            }
                        }
                    }

                    UUID placerUuid = placerStr != null ? UUID.fromString(placerStr) : null;
                    UUID targetUuid = targetStr != null ? UUID.fromString(targetStr) : null;
                    UUID killerUuid = killerStr != null ? UUID.fromString(killerStr) : null;

                    if (placerUuid != null && targetUuid != null) {
                        Bounty bounty = new Bounty(id, placerUuid, placerName, targetUuid, targetName, items, placedTs, killerUuid, killedTs, claimed);
                        bounties.add(bounty);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load bounty " + id + ": " + e.getMessage());
                }
            }
        }
    }

    public synchronized void saveBounties() {
        if (bountiesConfig == null || bountiesFile == null) return;
        bountiesConfig.set("bounties", null);

        for (Bounty bounty : bounties) {
            String path = "bounties." + bounty.getId();
            bountiesConfig.set(path + ".placerUuid", bounty.getPlacerUuid().toString());
            bountiesConfig.set(path + ".placerName", bounty.getPlacerName());
            bountiesConfig.set(path + ".targetUuid", bounty.getTargetUuid().toString());
            bountiesConfig.set(path + ".targetName", bounty.getTargetName());
            bountiesConfig.set(path + ".placedTimestamp", bounty.getPlacedTimestamp());
            if (bounty.getKillerUuid() != null) {
                bountiesConfig.set(path + ".killerUuid", bounty.getKillerUuid().toString());
            }
            bountiesConfig.set(path + ".killedTimestamp", bounty.getKilledTimestamp());
            bountiesConfig.set(path + ".claimed", bounty.isClaimed());
            bountiesConfig.set(path + ".items", bounty.getItems());
        }

        try {
            bountiesConfig.save(bountiesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save bounties.yml: " + e.getMessage());
        }
    }

    public synchronized void createBounty(Player placer, Player target, List<ItemStack> items) {
        if (items == null || items.isEmpty()) return;

        Bounty bounty = new Bounty(
                UUID.randomUUID().toString(),
                placer.getUniqueId(),
                placer.getName(),
                target.getUniqueId(),
                target.getName(),
                new ArrayList<>(items),
                System.currentTimeMillis(),
                null,
                0L,
                false
        );

        bounties.add(bounty);
        saveBounties();
    }

    public synchronized List<Bounty> getActiveBounties() {
        List<Bounty> list = new ArrayList<>();
        for (Bounty b : bounties) {
            if (b.isActive()) {
                list.add(b);
            }
        }
        return list;
    }

    public synchronized Map<UUID, List<Bounty>> getActiveBountiesGroupedByTarget() {
        Map<UUID, List<Bounty>> map = new LinkedHashMap<>();
        for (Bounty b : bounties) {
            if (b.isActive()) {
                map.computeIfAbsent(b.getTargetUuid(), k -> new ArrayList<>()).add(b);
            }
        }
        return map;
    }

    public synchronized List<Bounty> getActiveBountiesForTarget(UUID targetUuid) {
        List<Bounty> list = new ArrayList<>();
        for (Bounty b : bounties) {
            if (b.isActive() && b.getTargetUuid().equals(targetUuid)) {
                list.add(b);
            }
        }
        return list;
    }

    public synchronized boolean onPlayerKilled(Player victim, Player killer) {
        List<Bounty> active = getActiveBountiesForTarget(victim.getUniqueId());
        if (active.isEmpty()) return false;

        long now = System.currentTimeMillis();
        for (Bounty b : active) {
            b.setKillerUuid(killer.getUniqueId());
            b.setKilledTimestamp(now);
        }
        saveBounties();
        return true;
    }

    public synchronized List<Bounty> getClaimableBountiesForPlayer(Player player) {
        List<Bounty> list = new ArrayList<>();
        UUID uuid = player.getUniqueId();
        for (Bounty b : bounties) {
            if (b.isClaimableByKiller(uuid) || b.isRefundableToPlacer(uuid)) {
                list.add(b);
            }
        }
        return list;
    }

    public synchronized boolean claimOrRefundBounty(Bounty bounty, Player claimer) {
        if (bounty == null || bounty.isClaimed()) return false;

        UUID claimerUuid = claimer.getUniqueId();
        boolean isKiller = bounty.isClaimableByKiller(claimerUuid);
        boolean isRefund = bounty.isRefundableToPlacer(claimerUuid);

        if (!isKiller && !isRefund) return false;

        // Give items to claimer
        for (ItemStack item : bounty.getItems()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                var remaining = claimer.getInventory().addItem(item.clone());
                for (ItemStack rem : remaining.values()) {
                    claimer.getWorld().dropItemNaturally(claimer.getLocation(), rem);
                }
            }
        }

        bounty.setClaimed(true);
        saveBounties();
        return true;
    }

    public List<Bounty> getBounties() {
        return bounties;
    }
}
