package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.christmas.KrampusManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.List;
import java.util.Random;

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

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (christmasConfig == null || !christmasConfig.getBoolean("krampus.enabled"))
            return;

        // Boss Abilities
        if (event.getDamager() instanceof WitherSkeleton && event.getDamager().getCustomName() != null) {
            WitherSkeleton krampus = (WitherSkeleton) event.getDamager();
            if (krampus.getCustomName().contains("Krampus") && event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();

                // Blindness
                if (random.nextDouble() < 0.2) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
                    player.sendMessage(Component.text("Darkness consumes you...", NamedTextColor.DARK_GRAY));
                }

                // Kidnap Check (Lethal Damage)
                if (player.getHealth() - event.getFinalDamage() <= 0) {
                    event.setCancelled(true);
                    player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()); // Heal
                    krampusManager.kidnapPlayer(player);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (christmasConfig == null || !christmasConfig.getBoolean("krampus.enabled"))
            return;

        LivingEntity entity = event.getEntity();

        // Krampus Death
        if (entity instanceof WitherSkeleton && entity.getCustomName() != null
                && entity.getCustomName().contains("Krampus")) {
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
        if (entity instanceof Zombie && entity.getCustomName() != null
                && entity.getCustomName().contains("Cage Guard")) {
            if (entity.getKiller() != null) {
                Player killer = entity.getKiller();
                if (krampusManager.isKidnapped(killer)) {
                    krampusManager.releasePlayer(killer);
                }
            }
        }
    }
}
