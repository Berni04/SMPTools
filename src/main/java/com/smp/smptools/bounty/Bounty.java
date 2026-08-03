package com.smp.smptools.bounty;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Bounty {

    private final String id;
    private final UUID placerUuid;
    private final String placerName;
    private final UUID targetUuid;
    private final String targetName;
    private final List<ItemStack> items;
    private final long placedTimestamp;

    private UUID killerUuid;
    private long killedTimestamp;
    private boolean claimed;

    public static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000L;

    public Bounty(String id, UUID placerUuid, String placerName, UUID targetUuid, String targetName,
                  List<ItemStack> items, long placedTimestamp, UUID killerUuid, long killedTimestamp, boolean claimed) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.placerUuid = placerUuid;
        this.placerName = placerName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.items = items != null ? items : new ArrayList<>();
        this.placedTimestamp = placedTimestamp > 0 ? placedTimestamp : System.currentTimeMillis();
        this.killerUuid = killerUuid;
        this.killedTimestamp = killedTimestamp;
        this.claimed = claimed;
    }

    public String getId() { return id; }
    public UUID getPlacerUuid() { return placerUuid; }
    public String getPlacerName() { return placerName; }
    public UUID getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }
    public List<ItemStack> getItems() { return items; }
    public long getPlacedTimestamp() { return placedTimestamp; }
    public UUID getKillerUuid() { return killerUuid; }
    public void setKillerUuid(UUID killerUuid) { this.killerUuid = killerUuid; }
    public long getKilledTimestamp() { return killedTimestamp; }
    public void setKilledTimestamp(long killedTimestamp) { this.killedTimestamp = killedTimestamp; }
    public boolean isClaimed() { return claimed; }
    public void setClaimed(boolean claimed) { this.claimed = claimed; }

    public boolean isActive() {
        return killerUuid == null && !claimed;
    }

    public boolean isExpired() {
        return killerUuid != null && (System.currentTimeMillis() - killedTimestamp >= SEVEN_DAYS_MS);
    }

    public boolean isClaimableByKiller(UUID killer) {
        return !claimed && killerUuid != null && killerUuid.equals(killer) && !isExpired();
    }

    public boolean isRefundableToPlacer(UUID placer) {
        return !claimed && killerUuid != null && placerUuid.equals(placer) && isExpired();
    }
}
