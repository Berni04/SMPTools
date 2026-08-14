package com.smp.smptools.events.minievents;

import org.bukkit.Material;

/**
 * Enumeration of available server-wide automated mini-event types.
 */
public enum MiniEventType {
    FISHING_DERBY("Fishing Derby", "🎣", Material.FISHING_ROD, "Catch fish and treasure to score points!"),
    ORE_RUSH("Ore Rush", "⛏️", Material.DIAMOND_PICKAXE, "Mine ores for double drops and event points!"),
    MOB_FRENZY("Mob Frenzy", "⚔️", Material.NETHERITE_SWORD, "Slay hostile mobs for double drops and points!"),
    DOUBLE_XP("Double XP Hour", "⚡", Material.EXPERIENCE_BOTTLE, "Earn 2.0x Skill XP across all MMO skills!"),
    HARVEST_SPRINT("Harvest Sprint", "🌾", Material.DIAMOND_HOE, "Harvest mature crops with Speed II for bonus points!"),
    TREASURE_DIG("Treasure Dig", "🗺️", Material.DIAMOND_SHOVEL, "Excavate sand & gravel to uncover buried treasure!");

    private final String displayName;
    private final String iconEmoji;
    private final Material guiMaterial;
    private final String description;

    MiniEventType(String displayName, String iconEmoji, Material guiMaterial, String description) {
        this.displayName = displayName;
        this.iconEmoji = iconEmoji;
        this.guiMaterial = guiMaterial;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public Material getGuiMaterial() {
        return guiMaterial;
    }

    public String getDescription() {
        return description;
    }

    public String getFormattedName() {
        return iconEmoji + " " + displayName;
    }

    public String getConfigKey() {
        return name().toLowerCase();
    }
}
