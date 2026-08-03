package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.listeners.LeaderboardHubHolder;
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

public class LeaderboardCommand extends AbstractPlayerCommand {

    public LeaderboardCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        openLeaderboardHub(player);
        return true;
    }

    private void openLeaderboardHub(Player player) {
        LeaderboardHubHolder holder = new LeaderboardHubHolder();
        Inventory hubGUI = Bukkit.createInventory(holder, 54, plugin.getMessageManager().getMessage("leaderboard.gui-title", player));
        holder.setInventory(hubGUI);

        createDisplayItem(hubGUI, Material.CLOCK, 20, Component.text("Playtime", NamedTextColor.GOLD), "playtime");
        createDisplayItem(hubGUI, Material.DIAMOND_SWORD, 21, Component.text("Player Kills", NamedTextColor.GOLD), "player_kills");
        createDisplayItem(hubGUI, Material.SKELETON_SKULL, 22, Component.text("Total Deaths", NamedTextColor.GOLD), "deaths");
        createDisplayItem(hubGUI, Material.DIAMOND_PICKAXE, 23, Component.text("Blocks Broken", NamedTextColor.GOLD), "blocks_broken");
        createDisplayItem(hubGUI, Material.GRASS_BLOCK, 24, Component.text("Blocks Placed", NamedTextColor.GOLD), "blocks_placed");

        createDisplayItem(hubGUI, Material.COAL_ORE, 37, Component.text("Coal Mined", NamedTextColor.GOLD), "ores_mined.coal");
        createDisplayItem(hubGUI, Material.IRON_ORE, 38, Component.text("Iron Mined", NamedTextColor.GOLD), "ores_mined.iron");
        createDisplayItem(hubGUI, Material.GOLD_ORE, 39, Component.text("Gold Mined", NamedTextColor.GOLD), "ores_mined.gold");
        createDisplayItem(hubGUI, Material.LAPIS_LAZULI, 40, Component.text("Lapis Mined", NamedTextColor.GOLD), "ores_mined.lapis");
        createDisplayItem(hubGUI, Material.REDSTONE, 41, Component.text("Redstone Mined", NamedTextColor.GOLD), "ores_mined.redstone");
        createDisplayItem(hubGUI, Material.DIAMOND, 42, Component.text("Diamonds Mined", NamedTextColor.GOLD), "ores_mined.diamond");
        createDisplayItem(hubGUI, Material.EMERALD, 43, Component.text("Emeralds Mined", NamedTextColor.GOLD), "ores_mined.emerald");

        player.openInventory(hubGUI);
    }

    private void createDisplayItem(Inventory inv, Material material, int slot, Component name, String statKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(statKey, NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }
}
