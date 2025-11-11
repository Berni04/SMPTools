package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.skills.SkillType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final SMPTools plugin;

    public CombatListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        if (!isHostile(event.getEntityType())) {
            return;
        }

        Player player = (Player) event.getDamager();
        EntityType damagedEntityType = event.getEntityType();

        // Add Combat XP
        int xp = plugin.getSkillsManager().getExperienceForCombat(damagedEntityType);
        if (xp > 0) {
            plugin.getSkillsManager().addExperience(player, SkillType.COMBAT, xp);
        }

        // Attempt Critical Strike
        double newDamage = plugin.getSkillsManager().attemptCriticalStrike(player, event.getDamage());
        event.setDamage(newDamage);
    }

    private boolean isHostile(EntityType type) {
        return type == EntityType.ZOMBIE ||
               type == EntityType.SKELETON ||
               type == EntityType.CREEPER ||
               type == EntityType.SPIDER ||
               type == EntityType.CAVE_SPIDER ||
               type == EntityType.ENDERMAN ||
               type == EntityType.BLAZE ||
               type == EntityType.GHAST ||
               type == EntityType.MAGMA_CUBE ||
               type == EntityType.SLIME ||
               type == EntityType.WITCH ||
               type == EntityType.GUARDIAN ||
               type == EntityType.ELDER_GUARDIAN ||
               type == EntityType.SHULKER ||
               type == EntityType.VEX ||
               type == EntityType.EVOKER ||
               type == EntityType.VINDICATOR ||
               type == EntityType.RAVAGER ||
               type == EntityType.HOGLIN ||
               type == EntityType.ZOGLIN ||
               type == EntityType.PIGLIN_BRUTE ||
               type == EntityType.WITHER_SKELETON ||
               type == EntityType.WITHER ||
               type == EntityType.ENDER_DRAGON ||
               type == EntityType.WARDEN;
    }
}
