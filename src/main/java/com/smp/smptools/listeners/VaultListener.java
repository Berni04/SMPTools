package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.PrivateVaultCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public class VaultListener implements Listener {

    private final SMPTools plugin;

    public VaultListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().title().equals(Component.text("Private Vault", TextColor.fromHexString("#5B2C6F")))) {
            Player player = (Player) event.getPlayer();
            Inventory inventory = event.getInventory();
            FileConfiguration config = plugin.getConfig();

            try {
                String encodedInventory = PrivateVaultCommand.encodeInventory(inventory.getContents());
                config.set("privatevaults." + player.getUniqueId().toString(), encodedInventory);
                plugin.saveConfig();
                player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("vault.saved", player));
            } catch (IllegalStateException e) {
                plugin.getLogger().warning("Failed to encode inventory for player " + player.getName() + ": " + e.getMessage());
                player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("vault.save-failed", player));
            }
        }
    }
}
