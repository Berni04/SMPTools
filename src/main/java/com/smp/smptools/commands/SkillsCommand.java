package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.skills.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        openSkillsGUI((Player) sender);
        return true;
    }

    private void openSkillsGUI(Player player) {
        Inventory skillsGUI = Bukkit.createInventory(null, 27, Component.text("Your Skills"));

        int slot = 11; // Start in the middle of the top row
        for (SkillType skill : SkillType.values()) {
            int level = plugin.getSkillsManager().getLevel(player, skill);
            int currentXp = plugin.getSkillsManager().getCurrentExperience(player, skill);
            int xpToNextLevel = plugin.getSkillsManager().getExpToNextLevel(level);

            ItemStack skillItem = new ItemStack(getMaterialForSkill(skill));
            ItemMeta meta = skillItem.getItemMeta();

            meta.displayName(Component.text(skill.getDisplayName() + " - Level " + level, NamedTextColor.GREEN));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Experience: ", NamedTextColor.GRAY)
                    .append(Component.text(currentXp + " / " + xpToNextLevel, NamedTextColor.YELLOW)));
            lore.add(generateProgressBar(currentXp, xpToNextLevel));
            meta.lore(lore);

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

    private Component generateProgressBar(int current, int max) {
        if (max == 0) return Component.empty();
        float percent = (float) current / max;
        int barWidth = 20; // The total width of the progress bar in characters

        StringBuilder progressBar = new StringBuilder();
        for (int i = 0; i < barWidth; i++) {
            if (i < percent * barWidth) {
                progressBar.append("|");
            } else {
                progressBar.append("|");
            }
        }

        String filled = progressBar.substring(0, (int) (percent * barWidth));
        String empty = progressBar.substring((int) (percent * barWidth));

        return Component.text(filled, NamedTextColor.GREEN)
                .append(Component.text(empty, NamedTextColor.GRAY));
    }
}
