package com.smp.smptools.events.seasonal.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.events.seasonal.SeasonType;
import com.smp.smptools.events.seasonal.SeasonalManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Command handler for /summer.
 */
public class SummerCommand implements CommandExecutor {

    private final SMPTools plugin;
    private final SeasonalManager seasonalManager;

    public SummerCommand(SMPTools plugin, SeasonalManager seasonalManager) {
        this.plugin = plugin;
        this.seasonalManager = seasonalManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean active = seasonalManager.isSeasonActive(SeasonType.SUMMER);
        String status = active ? "<green>ACTIVE</green>" : "<red>INACTIVE (Active Jul 1 - Aug 31)</red>";

        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                "\n<gold>====================================</gold>\n" +
                "<yellow><b>☀️ Summer Heatwave & Solar Flares</b></yellow>\n" +
                "<gray>Status: " + status + "</gray>\n" +
                "<gray>• During midday under open skies, players receive Haste II & Speed I!</gray>\n" +
                "<gold>====================================</gold>\n"
        ));
        return true;
    }
}
