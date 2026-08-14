package com.smp.smptools.events.seasonal;

import org.bukkit.Material;

/**
 * Enum defining all recognized Seasonal Event types in SMPTools.
 */
public enum SeasonType {
    HALLOWEEN("Halloween Spooky Season", "🎃", Material.JACK_O_LANTERN, "Spooky Pumpkin Hunt, Headless Horseman & Trick-or-Treating!"),
    EASTER("Easter Spring Festival", "🐣", Material.EGG, "Hidden Egg Hunt, Golden Bunny spawns & Spring rewards!"),
    CHRISTMAS("Christmas & Winter Fest", "🎄", Material.SNOWBALL, "Advent Calendar, Secret Santa, Present Hunt, and Krampus!"),
    BLACK_FRIDAY("Black Friday Super Sale", "🛍️", Material.EMERALD, "Massive 90% villager trade discounts across the server!"),
    SUMMER("Summer Heatwave", "☀️", Material.SUNFLOWER, "Solar Flare buffs, Sun Haste, and Phoenix encounters!"),
    NONE("Regular Season", "🌿", Material.GRASS_BLOCK, "No seasonal event active. Standard SMP gameplay.");

    private final String displayName;
    private final String iconEmoji;
    private final Material guiMaterial;
    private final String description;

    SeasonType(String displayName, String iconEmoji, Material guiMaterial, String description) {
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
}
