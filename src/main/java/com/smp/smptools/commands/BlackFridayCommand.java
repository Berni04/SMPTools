package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.managers.BlackFridayManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BlackFridayCommand implements CommandExecutor {

    private final BlackFridayManager manager;

    public BlackFridayCommand(BlackFridayManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
            case "enable":
                manager.setEnabled(true);
                manager.broadcastToggle(true);
                sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("blackfriday.enabled"));
                break;

            case "stop":
            case "disable":
                manager.setEnabled(false);
                manager.broadcastToggle(false);
                sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("blackfriday.disabled"));
                break;

            case "reload":
                manager.reloadConfig();
                sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("blackfriday.reloaded"));
                break;

            case "status":
                boolean enabled = manager.isEnabled();
                int discount = manager.getDiscountPercentage();
                sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("blackfriday.status-header"));
                sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("blackfriday.status-enabled", null,
                        java.util.Map.of("status", SMPTools.getInstance().getMessageManager().getMessage(enabled ? "blackfriday.status-enabled-yes" : "blackfriday.status-enabled-no"))));
                sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("blackfriday.status-discount", null,
                        java.util.Map.of("discount", String.valueOf(discount))));
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("blackfriday.help-header"));
        sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("blackfriday.help-start"));
        sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("blackfriday.help-stop"));
        sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("blackfriday.help-reload"));
        sender.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("blackfriday.help-status"));
    }
}
