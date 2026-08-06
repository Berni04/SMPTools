package com.smp.smptools.bounty;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
    private final Map<UUID, List<ItemStack>> pendingRefunds = new ConcurrentHashMap<>();
    private File bountiesFile;
    private FileConfiguration bountiesConfig;
    private final Object fileLock = new Object();

    public BountyManager(SMPTools plugin) {
        this.plugin = plugin;
        loadBounties();
        startPeriodicCheck();
    }

    private void startPeriodicCheck() {
        if (plugin != null && plugin.isEnabled()) {
            try {
                Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpiredBounties, 1200L, 1200L);
            } catch (Exception ignored) {
                // In testing environment Bukkit scheduler might not be initialized
            }
        }
    }

    public synchronized void loadBounties() {
        if (plugin == null) return;
        bounties.clear();
        pendingRefunds.clear();

        bountiesFile = new File(plugin.getDataFolder(), "bounties.yml");
        if (!bountiesFile.exists()) {
            try {
                File parent = bountiesFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
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

        ConfigurationSection refundSec = bountiesConfig.getConfigurationSection("pendingRefunds");
        if (refundSec != null) {
            for (String uuidStr : refundSec.getKeys(false)) {
                try {
                    UUID placerUuid = UUID.fromString(uuidStr);
                    List<?> itemsRaw = refundSec.getList(uuidStr);
                    List<ItemStack> items = new ArrayList<>();
                    if (itemsRaw != null) {
                        for (Object obj : itemsRaw) {
                            if (obj instanceof ItemStack is) {
                                items.add(is);
                            }
                        }
                    }
                    if (!items.isEmpty()) {
                        pendingRefunds.put(placerUuid, items);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load pending refunds for " + uuidStr + ": " + e.getMessage());
                }
            }
        }
    }

    private List<Bounty> getBountiesSnapshot() {
        synchronized (this) {
            return new ArrayList<>(bounties);
        }
    }

    private Map<UUID, List<ItemStack>> getPendingRefundsSnapshot() {
        synchronized (this) {
            Map<UUID, List<ItemStack>> map = new HashMap<>();
            for (Map.Entry<UUID, List<ItemStack>> entry : pendingRefunds.entrySet()) {
                List<ItemStack> itemCopies = new ArrayList<>();
                for (ItemStack is : entry.getValue()) {
                    if (is != null) {
                        itemCopies.add(is.clone());
                    }
                }
                map.put(entry.getKey(), itemCopies);
            }
            return map;
        }
    }

    private void writeBountiesToFile(List<Bounty> snapshot, Map<UUID, List<ItemStack>> refundsSnapshot) {
        if (bountiesFile == null) return;
        synchronized (fileLock) {
            YamlConfiguration config = new YamlConfiguration();

            for (Bounty bounty : snapshot) {
                String path = "bounties." + bounty.getId();
                config.set(path + ".placerUuid", bounty.getPlacerUuid().toString());
                config.set(path + ".placerName", bounty.getPlacerName());
                config.set(path + ".targetUuid", bounty.getTargetUuid().toString());
                config.set(path + ".targetName", bounty.getTargetName());
                config.set(path + ".placedTimestamp", bounty.getPlacedTimestamp());
                if (bounty.getKillerUuid() != null) {
                    config.set(path + ".killerUuid", bounty.getKillerUuid().toString());
                }
                config.set(path + ".killedTimestamp", bounty.getKilledTimestamp());
                config.set(path + ".claimed", bounty.isClaimed());
                config.set(path + ".items", bounty.getItems());
            }

            for (Map.Entry<UUID, List<ItemStack>> entry : refundsSnapshot.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    config.set("pendingRefunds." + entry.getKey().toString(), entry.getValue());
                }
            }

            try {
                config.save(bountiesFile);
            } catch (IOException e) {
                if (plugin != null) {
                    plugin.getLogger().severe("Could not save bounties.yml: " + e.getMessage());
                }
            }
        }
    }

    public void saveBounties() {
        List<Bounty> snapshot = getBountiesSnapshot();
        Map<UUID, List<ItemStack>> refundsSnapshot = getPendingRefundsSnapshot();
        Runnable saveTask = () -> writeBountiesToFile(snapshot, refundsSnapshot);

        if (plugin != null && plugin.isEnabled()) {
            try {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
            } catch (Exception ignored) {
                saveTask.run();
            }
        } else {
            saveTask.run();
        }
    }

    public void saveBountiesSync() {
        List<Bounty> snapshot = getBountiesSnapshot();
        Map<UUID, List<ItemStack>> refundsSnapshot = getPendingRefundsSnapshot();
        writeBountiesToFile(snapshot, refundsSnapshot);
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

        // Atomic claim state persistence: mark claimed and save to disk BEFORE delivering items
        bounty.setClaimed(true);
        saveBountiesSync();

        // Give items to claimer
        for (ItemStack item : bounty.getItems()) {
            if (item != null && item.getType() != Material.AIR) {
                var remaining = claimer.getInventory().addItem(item.clone());
                for (ItemStack rem : remaining.values()) {
                    claimer.getWorld().dropItemNaturally(claimer.getLocation(), rem);
                }
            }
        }

        return true;
    }

    public synchronized void checkExpiredBounties() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (Bounty bounty : bounties) {
            if (!bounty.isClaimed() && bounty.getKillerUuid() != null && bounty.isExpired()) {
                bounty.setClaimed(true);
                changed = true;
                UUID placerUuid = bounty.getPlacerUuid();
                List<ItemStack> items = bounty.getItems();
                if (items != null && !items.isEmpty()) {
                    List<ItemStack> placerPending = pendingRefunds.computeIfAbsent(placerUuid, k -> new ArrayList<>());
                    for (ItemStack item : items) {
                        if (item != null && item.getType() != Material.AIR) {
                            placerPending.add(item.clone());
                        }
                    }
                    Player placer = Bukkit.getPlayer(placerUuid);
                    if (placer != null && placer.isOnline()) {
                        processPendingRefunds(placer);
                    }
                }
            }
        }
        if (changed) {
            saveBounties();
        }
    }

    public synchronized void processPendingRefunds(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        List<ItemStack> pending = pendingRefunds.get(uuid);
        if (pending == null || pending.isEmpty()) return;

        List<ItemStack> remainingPending = new ArrayList<>();
        for (ItemStack item : pending) {
            if (item == null || item.getType() == Material.AIR) continue;
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
            for (ItemStack rem : overflow.values()) {
                if (rem != null && rem.getType() != Material.AIR && rem.getAmount() > 0) {
                    remainingPending.add(rem);
                }
            }
        }

        if (remainingPending.isEmpty()) {
            pendingRefunds.remove(uuid);
        } else {
            pendingRefunds.put(uuid, remainingPending);
        }
        saveBounties();
    }

    public synchronized void checkPlayerJoin(Player player) {
        checkExpiredBounties();
        if (player != null && player.isOnline()) {
            processPendingRefunds(player);
        }
    }

    public synchronized Map<UUID, List<ItemStack>> getPendingRefunds() {
        Map<UUID, List<ItemStack>> copy = new HashMap<>();
        for (Map.Entry<UUID, List<ItemStack>> e : pendingRefunds.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableList(new ArrayList<>(e.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    public synchronized List<Bounty> getBounties() {
        return Collections.unmodifiableList(bounties);
    }
}
