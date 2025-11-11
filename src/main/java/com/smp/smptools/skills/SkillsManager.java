package com.smp.smptools.skills;

import com.smp.smptools.SMPTools;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

public class SkillsManager {

    private final SMPTools plugin;
    private final Random random = new Random();

    public SkillsManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public void addExperience(Player player, SkillType skill, int amount) {
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
            player.sendMessage("§aYour " + skill.getDisplayName() + " skill has reached level " + currentLevel + "!");
            expToNextLevel = getExpToNextLevel(currentLevel);
        }

        plugin.getStatsConfig().set(path + ".experience", newExp);
        plugin.getStatsConfig().set(path + ".level", currentLevel);
        plugin.saveStatsConfig();
    }

    public int getLevel(Player player, SkillType skill) {
        return plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".skills." + skill.name().toLowerCase() + ".level", 1);
    }

    public int getCurrentExperience(Player player, SkillType skill) {
        return plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".skills." + skill.name().toLowerCase() + ".experience", 0);
    }

    public boolean attemptDoubleDrop(Player player, SkillType skill) {
        if (!isSkillEnabled(skill)) {
            return false;
        }

        int level = getLevel(player, skill);
        String chanceFormula = plugin.getConfig().getString("features.mmo-skills." + skill.name().toLowerCase() + ".double-drop-chance");

        if (chanceFormula == null) {
            return false;
        }

        // Simple formula parser
        try {
            String[] parts = chanceFormula.split("\\*");
            double baseChance = Double.parseDouble(parts[0].trim());
            double chance = baseChance * level;
            return random.nextDouble() < chance;
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid double-drop-chance formula for " + skill.name() + ": " + chanceFormula);
            return false;
        }
    }

    public int getExpToNextLevel(int currentLevel) {
        // A simple exponential growth formula
        return (int) (100 * Math.pow(1.2, currentLevel - 1));
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
            String[] parts = chanceFormula.split("\\*");
            double baseChance = Double.parseDouble(parts[0].trim());
            double chance = baseChance * level;

            if (random.nextDouble() < chance) {
                // Success! Find a treasure.
                List<String> common = plugin.getConfig().getStringList("features.mmo-skills.excavation.treasure-hunter.loot.common");
                List<String> uncommon = plugin.getConfig().getStringList("features.mmo-skills.excavation.treasure-hunter.loot.uncommon");
                List<String> rare = plugin.getConfig().getStringList("features.mmo-skills.excavation.treasure-hunter.loot.rare");

                String itemString;
                double rarityRoll = random.nextDouble();

                if (rarityRoll < 0.05) { // 5% chance for rare
                    itemString = rare.get(random.nextInt(rare.size()));
                    player.sendMessage("§6§lRARE! §eYou found a rare treasure!");
                } else if (rarityRoll < 0.25) { // 20% chance for uncommon (25-5)
                    itemString = uncommon.get(random.nextInt(uncommon.size()));
                    player.sendMessage("§aYou found an uncommon treasure!");
                } else { // 75% chance for common
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
            // Calculate chance
            String[] chanceParts = chanceFormula.split("\\*");
            double baseChance = Double.parseDouble(chanceParts[0].trim());
            double chance = baseChance * level;

            if (random.nextDouble() < chance) {
                // Calculate damage multiplier
                double multiplier = 1.0;
                if (damageMultiplierFormula.contains("level")) {
                    multiplier = Double.parseDouble(damageMultiplierFormula.replace("level", String.valueOf(level)).replace(" ", ""));
                } else {
                    multiplier = Double.parseDouble(damageMultiplierFormula);
                }
                player.sendMessage("§c§lCRITICAL STRIKE! §r§7(" + String.format("%.1f", multiplier) + "x Damage)");
                return baseDamage * multiplier;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not execute Critical Strike due to an error: " + e.getMessage());
        }
        return baseDamage;
    }
}
