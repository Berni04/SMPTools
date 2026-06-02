package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.CommandBlacklist;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DailyRewardCommand extends AbstractPlayerCommand {

    public DailyRewardCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        String uuid = player.getUniqueId().toString();

        if (!plugin.getConfig().getBoolean("features.daily-rewards.enabled")) {
            player.sendMessage(plugin.getMessageManager().getMessage("daily.disabled"));
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
                player.sendMessage(plugin.getMessageManager().getMessage("daily.cooldown", player,
                        Map.of("hours", String.valueOf(hoursRemaining), "minutes", String.valueOf(minutesRemaining))));
                return true;
            }
        }

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
                String cmd = reward.replace("%player%", player.getName());
                if (CommandBlacklist.isBlocked(cmd)) {
                    plugin.getLogger().warning("Blocked dangerous command in daily reward: " + cmd);
                    continue;
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }

        plugin.getRewardsConfig().set("players." + uuid + ".last-claimed", Instant.now().toString());
        plugin.saveRewardsConfig();

        player.sendMessage(plugin.getMessageManager().getMessage("daily.claimed"));

        return true;
    }
}
