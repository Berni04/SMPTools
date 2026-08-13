package com.smp.smptools.artifacts;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Handles interactive triggers and passive ticks for all 21 Custom Utility Artifacts.
 */
public class ArtifactListener implements Listener {

    private final SMPTools plugin;
    private final ArtifactManager artifactManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> fallImmune = new HashSet<>();

    public ArtifactListener(SMPTools plugin, ArtifactManager artifactManager) {
        this.plugin = plugin;
        this.artifactManager = artifactManager;
        startPassiveTasks();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        ArtifactType type = artifactManager.getArtifactType(main);

        if (type == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            long now = System.currentTimeMillis();
            long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);

            switch (type) {
                case WIND_DASH_FEATHER:
                    if (now - last < 5000) {
                        player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Wind Dash on cooldown!</red>"));
                        return;
                    }
                    cooldowns.put(player.getUniqueId(), now);
                    Vector gaze = player.getLocation().getDirection().normalize();
                    player.setVelocity(gaze.multiply(2.0).setY(0.4));
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.2f);
                    grantFallImmunity(player, 5);
                    event.setCancelled(true);
                    break;

                case SHADOW_STEP_DAGGER:
                    if (now - last < 10000) {
                        player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Shadow Step on cooldown!</red>"));
                        return;
                    }
                    cooldowns.put(player.getUniqueId(), now);
                    Location dest = player.getLocation().add(player.getLocation().getDirection().multiply(8));
                    player.teleport(dest);
                    player.getWorld().spawnParticle(Particle.PORTAL, dest, 50, 0.5, 1.0, 0.5, 0.2);
                    player.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    event.setCancelled(true);
                    break;

                case PORTABLE_WORKBENCH:
                    player.openWorkbench(null, true);
                    player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1.0f, 1.2f);
                    event.setCancelled(true);
                    break;

                case VAMPIRIC_SCYTHE:
                    if (now - last < 20000) {
                        player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Vampiric Scythe on cooldown!</red>"));
                        return;
                    }
                    cooldowns.put(player.getUniqueId(), now);
                    double drained = 0;
                    for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                        if (entity instanceof LivingEntity target && target != player) {
                            double damage = target.getHealth() * 0.15;
                            target.damage(damage, player);
                            drained += damage;
                            target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, target.getLocation(), 20, 0.3, 0.5, 0.3, 0.1);
                        }
                    }
                    if (drained > 0) {
                        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + (drained * 0.5)));
                        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 1.5f);
                    }
                    event.setCancelled(true);
                    break;

                case SONIC_WAVE_HORN:
                    if (now - last < 15000) {
                        player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Sonic Horn on cooldown!</red>"));
                        return;
                    }
                    cooldowns.put(player.getUniqueId(), now);
                    Vector dir = player.getLocation().getDirection().normalize();
                    for (Entity entity : player.getNearbyEntities(8, 8, 8)) {
                        if (entity instanceof LivingEntity target && target != player) {
                            target.setVelocity(dir.multiply(2.5).setY(0.5));
                            target.damage(6.0, player);
                        }
                    }
                    player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getEyeLocation(), 1);
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1.0f, 1.0f);
                    event.setCancelled(true);
                    break;

                case DRAGON_BREATH_CANNON:
                    if (now - last < 15000) {
                        player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Dragon Breath Cannon on cooldown!</red>"));
                        return;
                    }
                    cooldowns.put(player.getUniqueId(), now);
                    DragonFireball fireball = player.launchProjectile(DragonFireball.class);
                    fireball.setVelocity(player.getLocation().getDirection().multiply(1.5));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
                    event.setCancelled(true);
                    break;

                case NATURES_TOUCH_HOE:
                    if (event.getClickedBlock() != null) {
                        Block clicked = event.getClickedBlock();
                        int harvested = 0;
                        for (int x = -2; x <= 2; x++) {
                            for (int z = -2; z <= 2; z++) {
                                Block crop = clicked.getRelative(x, 0, z);
                                if (crop.getBlockData() instanceof Ageable ageable && ageable.getAge() == ageable.getMaximumAge()) {
                                    crop.breakNaturally(main);
                                    ageable.setAge(0);
                                    crop.setBlockData(ageable);
                                    harvested++;
                                }
                            }
                        }
                        if (harvested > 0) {
                            player.playSound(player.getLocation(), Sound.ITEM_HOE_TILL, 1.0f, 1.2f);
                            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, clicked.getLocation().add(0.5, 1, 0.5), 15, 1.0, 0.5, 1.0);
                        }
                    }
                    break;

                default:
                    break;
            }
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();

        if (artifactManager.isArtifact(main, ArtifactType.GRAPPLING_HOOK)) {
            if (event.getState() == PlayerFishEvent.State.IN_GROUND || event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
                Location target = event.getHook().getLocation();
                Location pLoc = player.getLocation();

                Vector velocity = target.toVector().subtract(pLoc.toVector()).normalize().multiply(1.85).setY(0.4);
                player.setVelocity(velocity);
                player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
                grantFallImmunity(player, 5);
            }
        } else if (artifactManager.isArtifact(main, ArtifactType.MASTER_ANGLER_LURE)) {
            if (event.getState() == PlayerFishEvent.State.FISHING) {
                event.getHook().setWaitTime(event.getHook().getWaitTime() / 2);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL && fallImmune.contains(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            // Phoenix Feather Check
            if (player.getHealth() - event.getFinalDamage() <= 0) {
                if (artifactManager.hasEquippedArtifact(player, ArtifactType.PHOENIX_FEATHER)) {
                    event.setCancelled(true);
                    player.setHealth(player.getMaxHealth() * 0.5);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 4));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0));
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 50, 0.5, 1.0, 0.5, 0.2);
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                    player.sendActionBar(MiniMessage.miniMessage().deserialize("<gold><b>🔥 Phoenix Feather Resurrected You!</b></gold>"));
                }
            }
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (artifactManager.hasEquippedArtifact(player, ArtifactType.ALCHEMISTS_SATCHEL)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (PotionEffect effect : player.getActivePotionEffects()) {
                        player.addPotionEffect(new PotionEffect(effect.getType(), effect.getDuration() * 2, effect.getAmplifier()), true);
                    }
                }
            }.runTaskLater(plugin, 2L);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();

        if (artifactManager.isArtifact(main, ArtifactType.TIMBER_AXE)) {
            Block block = event.getBlock();
            if (block.getType().name().contains("LOG")) {
                fellTree(block, player, main);
            }
        }
    }

    private void fellTree(Block start, Player player, ItemStack tool) {
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();
        queue.add(start);

        int maxLogs = 64;
        int count = 0;

        while (!queue.isEmpty() && count < maxLogs) {
            Block current = queue.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            if (current.getType().name().contains("LOG")) {
                current.breakNaturally(tool);
                count++;

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            Block neighbor = current.getRelative(x, y, z);
                            if (!visited.contains(neighbor)) {
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
    }

    private void grantFallImmunity(Player player, int seconds) {
        fallImmune.add(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                fallImmune.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, seconds * 20L);
    }

    private void startPassiveTasks() {
        // Magnet Totem, Abyssal Lantern, Auto-Feeder, Chlorophyll Band task
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Abyssal Lantern
                    if (artifactManager.hasEquippedArtifact(player, ArtifactType.ABYSSAL_LANTERN)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 300, 0, false, false));
                    }

                    // Magnet Totem
                    if (artifactManager.hasEquippedArtifact(player, ArtifactType.MAGNET_TOTEM)) {
                        for (Entity entity : player.getNearbyEntities(12, 12, 12)) {
                            if (entity instanceof Item || entity instanceof ExperienceOrb) {
                                entity.setVelocity(player.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.4));
                            }
                        }
                    }

                    // Auto-Feeder Satchel
                    if (artifactManager.hasEquippedArtifact(player, ArtifactType.AUTO_FEEDER_SATCHEL) && player.getFoodLevel() < 16) {
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null && item.getType().isEdible()) {
                                player.setFoodLevel(20);
                                item.setAmount(item.getAmount() - 1);
                                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.0f);
                                break;
                            }
                        }
                    }

                    // Chlorophyll Band
                    if (artifactManager.hasEquippedArtifact(player, ArtifactType.CHLOROPHYLL_BAND)) {
                        Location pLoc = player.getLocation();
                        for (int x = -2; x <= 2; x++) {
                            for (int z = -2; z <= 2; z++) {
                                Block b = pLoc.getBlock().getRelative(x, -1, z);
                                Block crop = b.getRelative(0, 1, 0);
                                if (crop.getBlockData() instanceof Ageable ageable && ageable.getAge() < ageable.getMaximumAge()) {
                                    ageable.setAge(Math.min(ageable.getMaximumAge(), ageable.getAge() + 1));
                                    crop.setBlockData(ageable);
                                    crop.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, crop.getLocation().add(0.5, 0.5, 0.5), 3);
                                }
                            }
                        }
                    }

                    // Jack's Pumpkin Helmet
                    if (artifactManager.hasEquippedArtifact(player, ArtifactType.JACKS_PUMPKIN_HELMET)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 0, false, false));
                        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1.0, 0), 2, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
