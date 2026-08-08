package com.smp.smptools.artifacts;

import org.bukkit.Material;

/**
 * Enum defining all 21 Custom Utility Artifacts.
 */
public enum ArtifactType {
    // Mobility (5)
    GRAPPLING_HOOK("Grappling Hook", "🎣", Material.FISHING_ROD, ArtifactSlotType.ACTIVE, "Right-click to launch toward targeted block/mob with 5s fall damage immunity."),
    WIND_DASH_FEATHER("Wind Dash Feather", "🪶", Material.FEATHER, ArtifactSlotType.ACTIVE, "Right-click to dash forward in gaze direction with Elytra wind particles."),
    LEAP_FROG_BOOTS("Leap Frog Boots", "🐸", Material.LEATHER_BOOTS, ArtifactSlotType.PASSIVE, "Double-tap Jump to super-leap into the air with zero fall damage."),
    SHADOW_STEP_DAGGER("Shadow Step Dagger", "🗡️", Material.NETHERITE_SWORD, ArtifactSlotType.ACTIVE, "Right-click to teleport 8 blocks forward through doors & thin walls (10s cooldown)."),
    FEATHER_GLIDER_RING("Feather Glider Ring", "🪽", Material.FEATHER, ArtifactSlotType.PASSIVE, "Holding Shift while falling grants Slow Falling flight gliding."),

    // Utility & QoL (7)
    PORTABLE_WORKBENCH("Portable Workbench", "🧰", Material.CRAFTING_TABLE, ArtifactSlotType.ACTIVE, "Right-click to open 3x3 crafting grid anywhere in mid-air."),
    HOMING_COMPASS("Homing Compass", "🧭", Material.COMPASS, ArtifactSlotType.PASSIVE, "Shift+Right-Click cycles targets (Player, Grave, Boss) with Action Bar HUD."),
    MAGNET_TOTEM("Magnet Totem", "🧲", Material.TOTEM_OF_UNDYING, ArtifactSlotType.PASSIVE, "Pulls dropped items & XP orbs within 12 blocks directly to your feet."),
    ABYSSAL_LANTERN("Abyssal Lantern", "🏮", Material.SOUL_TORCH, ArtifactSlotType.PASSIVE, "Grants infinite Night Vision & Water Breathing while equipped."),
    VOID_SAVER_CHARM("Void Saver Charm", "🔮", Material.TOTEM_OF_UNDYING, ArtifactSlotType.PASSIVE, "Rescues you when falling into Void/Nether Lava, teleporting you to safety with Fire Res III."),
    ALCHEMISTS_SATCHEL("Alchemist's Satchel", "🧪", Material.BREWING_STAND, ArtifactSlotType.PASSIVE, "Automatically doubles the duration of all consumed potions."),
    AUTO_FEEDER_SATCHEL("Auto-Feeder Satchel", "🍞", Material.GOLDEN_CARROT, ArtifactSlotType.PASSIVE, "Auto-consumes food from inventory when hunger drops below 16."),

    // Combat & Defense (4)
    VAMPIRIC_SCYTHE("Vampiric Scythe", "🩸", Material.NETHERITE_HOE, ArtifactSlotType.ACTIVE, "Drains 15% HP from enemies within 5 blocks and heals the user (20s cooldown)."),
    PHOENIX_FEATHER("Phoenix Feather", "🔥", Material.FEATHER, ArtifactSlotType.PASSIVE, "Resurrects player on lethal damage with 50% HP + Reg V + knockback blast."),
    SONIC_WAVE_HORN("Sonic Wave Horn", "📯", Material.GOAT_HORN, ArtifactSlotType.ACTIVE, "Unleashes Warden sonic boom forward, knocking back enemies 10 blocks (15s cooldown)."),
    DRAGON_BREATH_CANNON("Dragon Breath Cannon", "🐲", Material.DRAGON_BREATH, ArtifactSlotType.ACTIVE, "Launches dragon breath fireball forward, leaving an AoE damage cloud (15s cooldown)."),

    // Gathering & Farming (5)
    NATURES_TOUCH_HOE("Nature's Touch Hoe", "🌿", Material.DIAMOND_HOE, ArtifactSlotType.ACTIVE, "Right-click harvests and replants a 5x5 mature crop field into inventory."),
    TIMBER_AXE("Timber Axe", "🪓", Material.NETHERITE_AXE, ArtifactSlotType.ACTIVE, "Chopping base log fells entire tree structure & auto-collects log drops."),
    ORE_RADAR_SCANNER("Ore Radar Scanner", "📡", Material.RECOVERY_COMPASS, ArtifactSlotType.PASSIVE, "Emits radar sound beeps that speed up near hidden Diamond/Debris ores."),
    CHLOROPHYLL_BAND("Chlorophyll Band", "💎", Material.EMERALD, ArtifactSlotType.PASSIVE, "Radiates a 5-block Bone Meal growth aura around you every 3 seconds."),
    MASTER_ANGLER_LURE("Master Angler's Lure", "🎣", Material.FISHING_ROD, ArtifactSlotType.ACTIVE, "Cuts fishing bite wait time in half + 25% chance for double catch drops.");

    private final String displayName;
    private final String iconEmoji;
    private final Material material;
    private final ArtifactSlotType slotType;
    private final String description;

    ArtifactType(String displayName, String iconEmoji, Material material, ArtifactSlotType slotType, String description) {
        this.displayName = displayName;
        this.iconEmoji = iconEmoji;
        this.material = material;
        this.slotType = slotType;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public Material getMaterial() {
        return material;
    }

    public ArtifactSlotType getSlotType() {
        return slotType;
    }

    public String getDescription() {
        return description;
    }

    public String getFormattedName() {
        return iconEmoji + " " + displayName;
    }

    public enum ArtifactSlotType {
        ACTIVE,
        PASSIVE
    }
}
