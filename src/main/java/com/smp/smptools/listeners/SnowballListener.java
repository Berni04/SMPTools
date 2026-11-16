package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (christmasConfig == null || !christmasConfig.getBoolean("snowball-warfare.enabled", false))
            return;

        if (event.getDamager() instanceof Snowball snowball && event.getEntity() instanceof LivingEntity victim) {
            String allowedWorld = christmasConfig.getString("snowball-warfare.world", "christmas");
            if (allowedWorld != null && !allowedWorld.isEmpty() && !victim.getWorld().getName().equalsIgnoreCase(allowedWorld)) {
                return;
            }

            if (victim instanceof Tameable tameable && tameable.isTamed()) {
                return;
            }
            if (victim instanceof Villager || victim instanceof WanderingTrader || victim instanceof ArmorStand) {
                return;
            }

            double damage = christmasConfig.getDouble("snowball-warfare.damage", 2.0);
            boolean isHeadshot = false;

            double eyeY = victim.getEyeLocation().getY();
            double ballY = snowball.getLocation().getY();
            if (ballY >= eyeY - 0.25 && ballY <= eyeY + 0.5) {
                damage *= christmasConfig.getDouble("snowball-warfare.headshot-multiplier", 2.0);
                isHeadshot = true;
            }

            event.setDamage(damage);

            int freezeTicks = christmasConfig.getInt("snowball-warfare.freeze-ticks", 40);
            victim.setFreezeTicks(Math.min(victim.getFreezeTicks() + freezeTicks, 140));

            if (snowball.getShooter() instanceof Player shooter) {
                if (isHeadshot) {
                    shooter.playSound(shooter.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
                    shooter.sendActionBar(plugin.getMessageManager().getMessage("christmas.headshot", shooter));
                } else {
                    shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f);
                }
            }
        }
    }
}
