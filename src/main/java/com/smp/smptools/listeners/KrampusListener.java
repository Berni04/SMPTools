package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.christmas.KrampusManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

public class KrampusListener implements Listener {

    private final SMPTools plugin;
    private final KrampusManager krampusManager;
    private final Random random = new Random();
    private FileConfiguration christmasConfig;

    public KrampusListener(SMPTools plugin, KrampusManager krampusManager) {
        this.plugin = plugin;
        this.krampusManager = krampusManager;
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "christmas.yml");
        if (file.exists()) {
            christmasConfig = YamlConfiguration.loadConfiguration(file);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (christmasConfig == null || !christmasConfig.getBoolean("krampus.enabled", false))
            return;

        // Boss Abilities
        if (event.getDamager() instanceof WitherSkeleton krampus) {
            if (krampus.getPersistentDataContainer().has(krampusManager.krampusKey, PersistentDataType.BYTE)
                    && event.getEntity() instanceof Player player) {

                // Debug Log (demoted to fine to avoid console spam)
                plugin.getLogger().log(Level.FINE, () -> "Krampus hit " + player.getName() + ". Health: " + player.getHealth()
                        + ", Damage: " + event.getFinalDamage());

                // Blindness
                if (random.nextDouble() < 0.2) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
                    player.sendMessage(plugin.getMessageManager().getMessage("krampus.darkness"));
                }

                // Kidnap Check (Lethal Damage)
                if (player.getHealth() - event.getFinalDamage() <= 0) {
                    plugin.getLogger().info("Krampus kidnapping triggered for " + player.getName());
                    event.setCancelled(true);
                    if (player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
                        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                    }
                    krampusManager.kidnapPlayer(player, krampus);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (krampusManager.isKidnapped(player)) {
            if (!krampusManager.isAllowedTeleport(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot teleport while trapped in Krampus's cage!</red>"));
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (christmasConfig == null || !christmasConfig.getBoolean("krampus.enabled", false))
            return;

        LivingEntity entity = event.getEntity();

        // Krampus Death
        if (entity instanceof WitherSkeleton
                && entity.getPersistentDataContainer().has(krampusManager.krampusKey, PersistentDataType.BYTE)) {
            event.getDrops().clear();
            List<String> drops = christmasConfig.getStringList("krampus.drops");
            for (String drop : drops) {
                Material mat = Material.getMaterial(drop);
                if (mat != null) {
                    event.getDrops().add(new ItemStack(mat, random.nextInt(3) + 1));
                }
            }
        }

        // Guard Death (Escape)
        UUID guardId = entity.getUniqueId();
        UUID victimUuid = krampusManager.getVictimForGuard(guardId);
        if (victimUuid != null) {
            krampusManager.checkGuardDeath(victimUuid, guardId);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (krampusManager.isKidnapped(event.getPlayer())) {
            krampusManager.releasePlayer(event.getPlayer());
        }
    }
}
