package com.smp.smptools.skills;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.Constants;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

/**
 * Manages the MMO-style skill system for players.
 * Handles experience gain, level progression, and skill-based abilities.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Exponential level progression formula</li>
 *   <li>Configurable double-drop chances for gathering skills</li>
 *   <li>Treasure hunter perk for excavation</li>
 *   <li>Critical strike chance for combat skill</li>
 * </ul>
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public class SkillsManager {

    private final SMPTools plugin;
    private final Random random = new Random();

    /**
     * Constructs a new SkillsManager.
     *
     * @param plugin the SMPTools plugin instance
     */
    public SkillsManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    /**
     * Adds experience to a player's skill and handles level-ups.
     *
     * @param player the player to add experience to
     * @param skill the skill type to add experience to
     * @param amount the amount of experience to add
     */
    public void addExperience(@NotNull Player player, @NotNull SkillType skill, int amount) {
        if (!isSkillEnabled(skill)) {
            return;
        }

        String uuid = player.getUniqueId().toString();
        String path = "stats." + uuid + ".skills." + skill.name().toLowerCase();

        int currentExp = plugin.getStatsConfig().getInt(path + ".experience", 0);
        int newExp = currentExp + amount;

        int currentLevel = plugin.getStatsConfig().getInt(path + ".level", 1);
        int expToNextLevel = getExpToNextLevel(currentLevel);

        while (newExp >= expToNextLevel) {
            newExp -= expToNextLevel;
            currentLevel++;
            player.sendMessage(plugin.getMessageManager().getMessage("skills.level-up", player,
                    java.util.Map.of("skill_name", skill.getDisplayName(), "level", String.valueOf(currentLevel))));
            expToNextLevel = getExpToNextLevel(currentLevel);
        }

        plugin.getStatsConfig().set(path + ".experience", newExp);
        plugin.getStatsConfig().set(path + ".level", currentLevel);
        plugin.saveStatsConfig();
    }

    /**
     * Gets the current level of a skill for a player.
     *
     * @param player the player to get the level for
     * @param skill the skill type to get the level for
     * @return the skill level (defaults to 1 if not found)
     */
    public int getLevel(@NotNull Player player, @NotNull SkillType skill) {
        return plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".skills." + skill.name().toLowerCase() + ".level", 1);
    }

    /**
     * Gets the current experience points in a skill for a player.
     *
     * @param player the player to get experience for
     * @param skill the skill type to get experience for
     * @return the current experience points (defaults to 0 if not found)
     */
    public int getCurrentExperience(@NotNull Player player, @NotNull SkillType skill) {
        return plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".skills." + skill.name().toLowerCase() + ".experience", 0);
    }

    /**
     * Attempts to trigger a double drop based on the player's skill level.
     *
     * @param player the player to check for double drops
     * @param skill the skill type to check
     * @return true if a double drop should occur, false otherwise
     */
    public boolean attemptDoubleDrop(@NotNull Player player, @NotNull SkillType skill) {
        if (!isSkillEnabled(skill)) {
            return false;
        }

        int level = getLevel(player, skill);
        String chanceFormula = plugin.getConfig().getString("features.mmo-skills." + skill.name().toLowerCase() + ".double-drop-chance");

        if (chanceFormula == null) {
            return false;
        }

        try {
            double chance = parseFormula(chanceFormula, level);
            return random.nextDouble() < chance;
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid double-drop-chance formula for " + skill.name() + ": " + chanceFormula);
            return false;
        }
    }

    /**
     * Calculates the experience required to reach the next level.
     * Uses an exponential growth formula: base * growthRate^(level-1)
     *
     * @param currentLevel the current skill level
     * @return the experience required for the next level
     */
    public int getExpToNextLevel(int currentLevel) {
        return (int) (Constants.SKILL_BASE_EXP * Math.pow(Constants.SKILL_GROWTH_RATE, currentLevel - 1));
    }

    private boolean isSkillEnabled(SkillType skill) {
        return plugin.getConfig().getBoolean("features.mmo-skills." + skill.name().toLowerCase() + ".enabled", false);
    }

    public int getExperienceForBlock(Material material) {
        switch (material) {
            // Common Blocks
            case STONE:
            case COBBLESTONE:
            case DEEPSLATE:
                return 1;

            // Ores
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return 5;
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return 8;
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return 10;
            case NETHER_GOLD_ORE:
                return 10;
            case NETHER_QUARTZ_ORE:
                return 12;
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return 15;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return 15;
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
                return 20;
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return 50;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return 100;
            case ANCIENT_DEBRIS:
                return 250;

            default:
                return 0;
        }
    }

    public int getExperienceForWoodcutting(Material material) {
        if (material.name().contains("LOG")) {
            return 3;
        }
        return 0;
    }

    public int getExperienceForExcavation(Material material) {
        switch (material) {
            case DIRT:
            case GRASS_BLOCK:
                return 2;
            case SAND:
            case GRAVEL:
                return 3;
            case SOUL_SAND:
            case SOUL_SOIL:
                return 4;
            case CLAY:
                return 5;
            default:
                return 0;
        }
    }

    public void handleTreasureHunt(Player player) {
        if (!plugin.getConfig().getBoolean("features.mmo-skills.excavation.treasure-hunter.enabled")) {
            return;
        }

        int level = getLevel(player, SkillType.EXCAVATION);
        String chanceFormula = plugin.getConfig().getString("features.mmo-skills.excavation.treasure-hunter.chance");
        if (chanceFormula == null) return;

        try {
            double chance = parseFormula(chanceFormula, level);

            if (random.nextDouble() < chance) {
                List<String> common = plugin.getConfig().getStringList("features.mmo-skills.excavation.treasure-hunter.loot.common");
                List<String> uncommon = plugin.getConfig().getStringList("features.mmo-skills.excavation.treasure-hunter.loot.uncommon");
                List<String> rare = plugin.getConfig().getStringList("features.mmo-skills.excavation.treasure-hunter.loot.rare");

                String itemString;
                double rarityRoll = random.nextDouble();

                if (rarityRoll < 0.05) {
                    itemString = rare.get(random.nextInt(rare.size()));
                    player.sendMessage(plugin.getMessageManager().getMessage("skills.rare-treasure"));
                } else if (rarityRoll < 0.25) {
                    itemString = uncommon.get(random.nextInt(uncommon.size()));
                    player.sendMessage(plugin.getMessageManager().getMessage("skills.uncommon-treasure"));
                } else {
                    itemString = common.get(random.nextInt(common.size()));
                }

                String[] itemParts = itemString.split(" ");
                Material material = Material.valueOf(itemParts[0]);
                int amount = Integer.parseInt(itemParts[1]);
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(material, amount));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not execute Treasure Hunt due to an error: " + e.getMessage());
        }
    }

    public int getExperienceForCombat(EntityType entityType) {
        switch (entityType) {
            case ZOMBIE:
            case SKELETON:
                return 5;
            case SPIDER:
            case CAVE_SPIDER:
                return 7;
            case CREEPER:
                return 10;
            case ENDERMAN:
                return 15;
            case BLAZE:
                return 20;
            case WITHER_SKELETON:
                return 25;
            case GHAST:
                return 30;
            case MAGMA_CUBE:
            case SLIME:
                return 3; // Smaller, less threatening
            case GUARDIAN:
                return 20;
            case ELDER_GUARDIAN:
                return 100;
            case SHULKER:
                return 25;
            case VEX:
                return 10;
            case EVOKER:
                return 50;
            case VINDICATOR:
                return 30;
            case RAVAGER:
                return 75;
            case HOGLIN:
            case ZOGLIN:
                return 15;
            case PIGLIN_BRUTE:
                return 30;
            case WITHER:
                return 500;
            case ENDER_DRAGON:
                return 1000;
            case WARDEN:
                return 2000;
            default:
                return 0;
        }
    }

    public double attemptCriticalStrike(Player player, double baseDamage) {
        if (!isSkillEnabled(SkillType.COMBAT) || !plugin.getConfig().getBoolean("features.mmo-skills.combat.critical-strike.enabled")) {
            return baseDamage;
        }

        int level = getLevel(player, SkillType.COMBAT);
        String chanceFormula = plugin.getConfig().getString("features.mmo-skills.combat.critical-strike.chance");
        String damageMultiplierFormula = plugin.getConfig().getString("features.mmo-skills.combat.critical-strike.damage-multiplier");

        if (chanceFormula == null || damageMultiplierFormula == null) {
            return baseDamage;
        }

        try {
            double chance = parseFormula(chanceFormula, level);

            if (random.nextDouble() < chance) {
                double multiplier = parseDamageMultiplier(damageMultiplierFormula, level);
                player.sendMessage(plugin.getMessageManager().getMessage("skills.critical-strike", player,
                        java.util.Map.of("multiplier", String.format("%.1f", multiplier))));
                return baseDamage * multiplier;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not execute Critical Strike due to an error: " + e.getMessage());
        }
        return baseDamage;
    }

    private double parseFormula(String formula, int level) {
        if (formula == null || formula.isEmpty()) {
            throw new IllegalArgumentException("Formula cannot be null or empty");
        }

        if (!formula.matches("\\d+\\.?\\d*\\s*\\*\\s*level")) {
            throw new IllegalArgumentException("Invalid formula format: " + formula);
        }

        String[] parts = formula.split("\\*");
        double base = Double.parseDouble(parts[0].trim());
        return base * level;
    }

    private double parseDamageMultiplier(String formula, int level) {
        if (formula.contains("level")) {
            return Double.parseDouble(formula.replace("level", String.valueOf(level)).replace(" ", ""));
        }
        return Double.parseDouble(formula);
    }
}
