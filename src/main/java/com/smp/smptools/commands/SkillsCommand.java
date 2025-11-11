package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.skills.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SkillsCommand implements CommandExecutor {

    private final SMPTools plugin;

    public SkillsCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        openSkillsGUI((Player) sender);
        return true;
    }

    private void openSkillsGUI(Player player) {
        Inventory skillsGUI = Bukkit.createInventory(null, 27, "Your Skills");

        int slot = 11; // Start in the middle of the top row
        for (SkillType skill : SkillType.values()) {
            int level = plugin.getSkillsManager().getLevel(player, skill);
            int currentXp = plugin.getSkillsManager().getCurrentExperience(player, skill);
            int xpToNextLevel = plugin.getSkillsManager().getExpToNextLevel(level);

            ItemStack skillItem = new ItemStack(getMaterialForSkill(skill));
            ItemMeta meta = skillItem.getItemMeta();

            meta.setDisplayName(ChatColor.GREEN + skill.getDisplayName() + " - Level " + level);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Experience: " + ChatColor.YELLOW + currentXp + " / " + xpToNextLevel);
            lore.add(generateProgressBar(currentXp, xpToNextLevel));
            meta.setLore(lore);

            skillItem.setItemMeta(meta);
            skillsGUI.setItem(slot, skillItem);
            slot += 2; // space them out
        }

        player.openInventory(skillsGUI);
    }

    private Material getMaterialForSkill(SkillType skill) {
        switch (skill) {
            case MINING: return Material.DIAMOND_PICKAXE;
            case WOODCUTTING: return Material.DIAMOND_AXE;
            case EXCAVATION: return Material.DIAMOND_SHOVEL;
            case COMBAT: return Material.DIAMOND_SWORD;
            default: return Material.STONE;
        }
    }

    private String generateProgressBar(int current, int max) {
        if (max == 0) return "";
        float percent = (float) current / max;
        int barWidth = 20; // The total width of the progress bar in characters

        StringBuilder progressBar = new StringBuilder();
        progressBar.append(ChatColor.GREEN);
        for (int i = 0; i < barWidth; i++) {
            if (i < percent * barWidth) {
                progressBar.append("|");
            } else {
                if (progressBar.toString().contains(ChatColor.GRAY.toString())) {
                    progressBar.append("|");
                } else {
                    progressBar.append(ChatColor.GRAY).append("|");
                }
            }
        }
        return progressBar.toString();
    }
}
