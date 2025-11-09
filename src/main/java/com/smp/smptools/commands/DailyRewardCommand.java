package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class DailyRewardCommand implements CommandExecutor {

    private final SMPTools plugin;

    public DailyRewardCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        String uuid = player.getUniqueId().toString();

        if (!plugin.getConfig().getBoolean("features.daily-rewards.enabled")) {
            player.sendMessage(ChatColor.RED + "The daily reward system is currently disabled.");
            return true;
        }

        String lastClaimedString = plugin.getRewardsConfig().getString("players." + uuid + ".last-claimed");
        long cooldownHours = plugin.getConfig().getLong("features.daily-rewards.cooldown-hours");

        if (lastClaimedString != null) {
            Instant lastClaimed = Instant.parse(lastClaimedString);
            Instant now = Instant.now();
            Duration timeSinceClaimed = Duration.between(lastClaimed, now);

            if (timeSinceClaimed.toHours() < cooldownHours) {
                long hoursRemaining = cooldownHours - timeSinceClaimed.toHours();
                long minutesRemaining = (cooldownHours * 60) - timeSinceClaimed.toMinutes();
                minutesRemaining %= 60;
                player.sendMessage(ChatColor.RED + "You have already claimed your daily reward. Please wait " + hoursRemaining + "h " + minutesRemaining + "m.");
                return true;
            }
        }

        // Grant rewards
        List<String> rewards = plugin.getConfig().getStringList("features.daily-rewards.rewards");
        for (String reward : rewards) {
            if (reward.startsWith("item:")) {
                try {
                    String[] parts = reward.substring(5).split(" ");
                    Material material = Material.valueOf(parts[0].toUpperCase());
                    int amount = Integer.parseInt(parts[1]);
                    player.getInventory().addItem(new ItemStack(material, amount));
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid item format in daily reward: " + reward);
                }
            } else {
                // Assume it's a command
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("%player%", player.getName()));
            }
        }

        plugin.getRewardsConfig().set("players." + uuid + ".last-claimed", Instant.now().toString());
        plugin.saveRewardsConfig();

        player.sendMessage(ChatColor.GREEN + "You have successfully claimed your daily reward!");

        return true;
    }
}
