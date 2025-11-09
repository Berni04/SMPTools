package com.smp.smptools.skills;

import com.smp.smptools.SMPTools;
import org.bukkit.entity.Player;

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

    private int getExpToNextLevel(int currentLevel) {
        // A simple exponential growth formula
        return (int) (100 * Math.pow(1.2, currentLevel - 1));
    }

    private boolean isSkillEnabled(SkillType skill) {
        return plugin.getConfig().getBoolean("features.mmo-skills." + skill.name().toLowerCase() + ".enabled", false);
    }
}
