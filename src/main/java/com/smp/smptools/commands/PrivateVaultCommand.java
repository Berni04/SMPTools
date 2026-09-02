package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

public class PrivateVaultCommand extends AbstractPlayerCommand {

    public PrivateVaultCommand(SMPTools plugin) {
        super(plugin);
    }

    public static File getVaultsFile(SMPTools plugin) {
        return new File(plugin != null ? plugin.getDataFolder() : new File("plugins/SMPTools"), "vaults.yml");
    }

    private static volatile YamlConfiguration cachedVaultsConfig = null;
    private static volatile boolean vaultsConfigLoadFailed = false;

    public static synchronized YamlConfiguration getVaultsConfig(SMPTools plugin) {
        if (cachedVaultsConfig != null) {
            return cachedVaultsConfig;
        }
        File file = getVaultsFile(plugin);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        try {
            YamlConfiguration config = new YamlConfiguration();
            if (file.exists()) {
                config.load(file);
            }
            cachedVaultsConfig = config;
            vaultsConfigLoadFailed = false;
        } catch (Exception e) {
            vaultsConfigLoadFailed = true;
            if (plugin != null) {
                plugin.getLogger().severe("Failed to load vaults.yml! Aborting vault saves to prevent file overwrite: " + e.getMessage());
            }
            cachedVaultsConfig = new YamlConfiguration();
        }
        return cachedVaultsConfig;
    }

    public static boolean isVaultsConfigLoadFailed() {
        return vaultsConfigLoadFailed;
    }

    public static synchronized void resetCachedConfig() {
        cachedVaultsConfig = null;
        vaultsConfigLoadFailed = false;
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

            com.smp.smptools.listeners.PrivateVaultHolder holder = new com.smp.smptools.listeners.PrivateVaultHolder();
            Inventory vault = Bukkit.createInventory(holder, vaultSize, plugin.getMessageManager().getMessage("vault.gui-title", player));
            holder.setInventory(vault);

            YamlConfiguration vaultsConfig = getVaultsConfig(plugin);
            String encodedInventory = vaultsConfig.getString("vaults." + playerUUID);
            // Backwards compatibility migration check in main config
            if (encodedInventory == null) {
                encodedInventory = config.getString("privatevaults." + playerUUID);
            }

            if (encodedInventory != null && !encodedInventory.isEmpty()) {
                try {
                    vault.setContents(decodeInventory(encodedInventory));
                } catch (IOException | ClassNotFoundException e) {
                    holder.setDecodeFailed(true);
                    plugin.getLogger().warning("Failed to decode vault for player " + player.getName() + ": " + e.getMessage());
                    player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Warning: Some items in your vault failed to decode properly.</red>"));
                } catch (IllegalArgumentException e) {
                    holder.setDecodeFailed(true);
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
            int length = dataInput.readInt();
            if (length < 0 || length > 216) {
                throw new IOException("Invalid vault item array length: " + length);
            }
            ItemStack[] items = new ItemStack[length];
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
