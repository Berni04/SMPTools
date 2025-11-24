package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.io.File;

public class SnowballListener implements Listener {

    private final SMPTools plugin;
    private FileConfiguration christmasConfig;

    public SnowballListener(SMPTools plugin) {
        this.plugin = plugin;
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
        if (christmasConfig == null || !christmasConfig.getBoolean("snowball-warfare.enabled"))
            return;

        if (event.getDamager() instanceof Snowball && event.getEntity() instanceof LivingEntity) {
            LivingEntity victim = (LivingEntity) event.getEntity();
            Snowball snowball = (Snowball) event.getDamager();

            double damage = christmasConfig.getDouble("snowball-warfare.damage", 2.0);
            boolean isHeadshot = false;

            // Simple headshot detection: Snowball Y is near Eye Location Y
            if (snowball.getLocation().getY() > victim.getEyeLocation().getY() - 0.3) {
                damage *= christmasConfig.getDouble("snowball-warfare.headshot-multiplier", 2.0);
                isHeadshot = true;
            }

            event.setDamage(damage);

            // Apply freeze
            int freezeTicks = christmasConfig.getInt("snowball-warfare.freeze-ticks", 40);
            victim.setFreezeTicks(Math.min(victim.getFreezeTicks() + freezeTicks, 140)); // Cap freeze ticks

            // Feedback
            if (snowball.getShooter() instanceof Player) {
                Player shooter = (Player) snowball.getShooter();
                if (isHeadshot) {
                    shooter.playSound(shooter.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
                    shooter.sendActionBar(Component.text("Headshot!", NamedTextColor.GOLD));
                } else {
                    shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f);
                }
            }
        }
    }
}
