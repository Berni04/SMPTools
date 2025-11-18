package com.smp.smptools.listeners;

import com.smp.smptools.missions.RewardManager;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ElytraTrailListener implements Listener {

    @EventHandler
    public void onPlayerGlide(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!player.isGliding()) {
            return;
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || !chestplate.hasItemMeta()) {
            return;
        }

        String trailColor = chestplate.getItemMeta().getPersistentDataContainer().get(RewardManager.ELYTRA_TRAIL_KEY, PersistentDataType.STRING);

        if (trailColor != null) {
            spawnTrailParticle(player, trailColor);
        }
    }

    private void spawnTrailParticle(Player player, String colorName) {
        try {
            Particle particle = Particle.DUST;
            Particle.DustOptions dustOptions = null;
            int count = 5;
            double speed = 0;

            switch (colorName.toLowerCase()) {
                case "red":
                    dustOptions = new Particle.DustOptions(Color.RED, 1.0F);
                    break;
                case "blue":
                    dustOptions = new Particle.DustOptions(Color.BLUE, 1.0F);
                    break;
                case "green":
                    dustOptions = new Particle.DustOptions(Color.LIME, 1.0F);
                    break;
                case "purple":
                    dustOptions = new Particle.DustOptions(Color.PURPLE, 1.0F);
                    break;
                case "orange":
                    dustOptions = new Particle.DustOptions(Color.ORANGE, 1.0F);
                    break;
                case "yellow":
                    dustOptions = new Particle.DustOptions(Color.YELLOW, 1.0F);
                    break;
                case "black":
                    dustOptions = new Particle.DustOptions(Color.BLACK, 1.0F);
                    break;
                case "white":
                    dustOptions = new Particle.DustOptions(Color.WHITE, 1.0F);
                    break;
                case "rainbow":
                    long time = System.currentTimeMillis() / 100;
                    float hue = (float) (time % 360) / 360.0f;
                    java.awt.Color rainbowColor = java.awt.Color.getHSBColor(hue, 1.0f, 1.0f);
                    dustOptions = new Particle.DustOptions(Color.fromRGB(rainbowColor.getRed(), rainbowColor.getGreen(), rainbowColor.getBlue()), 1.0F);
                    break;
                default:
                    particle = Particle.FLAME; // Fallback for unknown color string
                    break;
            }

            if (dustOptions != null) {
                player.getWorld().spawnParticle(particle, player.getLocation(), count, 0.1, 0.1, 0.1, speed, dustOptions);
            } else {
                player.getWorld().spawnParticle(particle, player.getLocation(), count, 0.1, 0.1, 0.1, speed);
            }

        } catch (Exception e) {
            // Graceful fallback if any particle logic fails
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 1, 0, 0, 0, 0);
        }
    }
}
