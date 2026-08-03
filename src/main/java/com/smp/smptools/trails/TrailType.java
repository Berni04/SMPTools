package com.smp.smptools.trails;

import org.bukkit.Material;
import org.bukkit.Particle;

public enum TrailType {
    FLAME("flame", "<gold>Flame Trail</gold>", Particle.FLAME, Material.BLAZE_POWDER, "smptools.trails.flame"),
    HEART("heart", "<red>Heart Trail</red>", Particle.HEART, Material.REDSTONE, "smptools.trails.heart"),
    ENCHANT("enchant", "<blue>Enchantment Swirls</blue>", Particle.ENCHANT, Material.ENCHANTING_TABLE, "smptools.trails.enchant"),
    END_ROD("end_rod", "<yellow>End Rod Sparkles</yellow>", Particle.END_ROD, Material.END_ROD, "smptools.trails.endrod"),
    DRAGON("dragon", "<purple>Dragon Breath</purple>", Particle.DRAGON_BREATH, Material.DRAGON_BREATH, "smptools.trails.dragon"),
    SOUL_FIRE("soul_fire", "<cyan>Soul Flame Trail</cyan>", Particle.SOUL_FIRE_FLAME, Material.SOUL_SOIL, "smptools.trails.soulfire"),
    TOTEM("totem", "<yellow>Totem Sparkles</yellow>", Particle.TOTEM_OF_UNDYING, Material.TOTEM_OF_UNDYING, "smptools.trails.totem");

    private final String id;
    private final String displayName;
    private final Particle particle;
    private final Material icon;
    private final String permission;

    TrailType(String id, String displayName, Particle particle, Material icon, String permission) {
        this.id = id;
        this.displayName = displayName;
        this.particle = particle;
        this.icon = icon;
        this.permission = permission;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Particle getParticle() { return particle; }
    public Material getIcon() { return icon; }
    public String getPermission() { return permission; }

    public static TrailType fromId(String id) {
        if (id == null) return null;
        for (TrailType t : values()) {
            if (t.getId().equalsIgnoreCase(id)) return t;
        }
        return null;
    }
}
