package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.skills.SkillType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SkillsCommand extends AbstractPlayerCommand {

    public SkillsCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        openSkillsGUI(player);
        return true;
    }

    private void openSkillsGUI(Player player) {
        Inventory skillsGUI = Bukkit.createInventory(null, 27, plugin.getMessageManager().getMessage("skills.gui-title", player));

        int slot = 11;
        for (SkillType skill : SkillType.values()) {
            int level = plugin.getSkillsManager().getLevel(player, skill);
            int currentXp = plugin.getSkillsManager().getCurrentExperience(player, skill);
            int xpToNextLevel = plugin.getSkillsManager().getExpToNextLevel(level);

            ItemStack skillItem = new ItemStack(getMaterialForSkill(skill));
            ItemMeta meta = skillItem.getItemMeta();

            meta.displayName(plugin.getMessageManager().getMessage("skills.gui-display-name", player,
                    Map.of("name", skill.getDisplayName(), "level", String.valueOf(level))));

            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getMessageManager().getMessage("skills.gui-experience", player,
                    Map.of("current", String.valueOf(currentXp), "next", String.valueOf(xpToNextLevel))));
            lore.add(generateProgressBar(currentXp, xpToNextLevel));
            meta.lore(lore);

            skillItem.setItemMeta(meta);
            skillsGUI.setItem(slot, skillItem);
            slot += 2;
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
        int barWidth = 20;

        String filled = "█".repeat((int) (percent * barWidth));
        String empty = "█".repeat(barWidth - (int) (percent * barWidth));

        return Component.text(filled, NamedTextColor.GREEN)
                .append(Component.text(empty, NamedTextColor.GRAY));
    }
}
