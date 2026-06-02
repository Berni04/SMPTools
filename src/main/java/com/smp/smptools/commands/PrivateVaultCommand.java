package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

public class PrivateVaultCommand extends AbstractPlayerCommand {

    public PrivateVaultCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        UUID playerUUID = player.getUniqueId();
        FileConfiguration config = plugin.getConfig();

        if (player.hasPermission("smptools.privatevault")) {
            int vaultSize = config.getInt("private-vault-size", 54);
            if (vaultSize % 9 != 0 || vaultSize < 9 || vaultSize > 54) {
                plugin.getLogger().warning("Invalid private-vault-size in config.yml. Using default size of 54.");
                vaultSize = 54;
            }

            Inventory vault = Bukkit.createInventory(null, vaultSize, plugin.getMessageManager().getMessage("vault.gui-title", player));

            String encodedInventory = config.getString("privatevaults." + playerUUID.toString());
            if (encodedInventory != null && !encodedInventory.isEmpty()) {
                try {
                    vault.setContents(decodeInventory(encodedInventory));
                } catch (IOException | ClassNotFoundException e) {
                    plugin.getLogger().warning("Failed to decode inventory for player " + player.getName() + ": " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid Base64 string for player " + player.getName() + ": " + e.getMessage());
                }
            }

            player.openInventory(vault);
            player.sendMessage(plugin.getMessageManager().getMessage("vault.opened"));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
        }
        return true;
    }

    public static String encodeInventory(ItemStack[] items) throws IllegalStateException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack[] decodeInventory(String data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid Base64 string.", e);
        }
    }
}
