package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.PrivateVaultCommand;
import com.smp.smptools.utils.AtomicFileWriter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;

public class VaultListener implements Listener {

    private final SMPTools plugin;

    public VaultListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof PrivateVaultHolder holder) {
            Player player = (Player) event.getPlayer();
            Inventory inventory = event.getInventory();

            if (holder.isDecodeFailed() || PrivateVaultCommand.isVaultsConfigLoadFailed()) {
                plugin.getLogger().warning("Aborted vault save for " + player.getName() + " due to previous decode or config load failure.");
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>🔒 Vault save aborted to protect existing data from decode errors. Contact an admin.</red>"));
                return;
            }

            try {
                String encodedInventory = PrivateVaultCommand.encodeInventory(inventory.getContents());
                File vaultsFile = new File(plugin.getDataFolder(), "vaults.yml");
                YamlConfiguration vaultsConfig = PrivateVaultCommand.getVaultsConfig(plugin);
                vaultsConfig.set("vaults." + player.getUniqueId(), encodedInventory);
                com.smp.smptools.utils.AsyncConfigHelper.saveConfigAsync(plugin, vaultsConfig, vaultsFile, "vaults.yml");

                player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("vault.saved", player));
            } catch (IllegalStateException e) {
                plugin.getLogger().warning("Failed to save vault for player " + player.getName() + ": " + e.getMessage());
                player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("vault.save-failed", player));
            }
        }
    }
}
