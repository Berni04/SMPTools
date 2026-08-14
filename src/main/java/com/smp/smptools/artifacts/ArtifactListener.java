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
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Handles interactive triggers and passive ticks for all Custom Utility Artifacts.
 */
public class ArtifactListener implements Listener {

    private final SMPTools plugin;
    private final ArtifactManager artifactManager;
    private final NamespacedKey bootsFlightKey;
    private final Map<UUID, Map<ArtifactType, Long>> cooldowns = new HashMap<>();
    private final Set<UUID> fallImmune = new HashSet<>();
    private final Set<UUID> bootsFlightGranted = new HashSet<>();
    private final Map<UUID, Integer> homingCompassTargetMode = new HashMap<>(); // 0: Nearest Player, 1: Nearest Boss, 2: Grave/Death
    private static final Set<Block> currentlyFelling = Collections.synchronizedSet(new HashSet<>());
    private final Random random = new Random();

    public static boolean isFelling(Block block) {
        return block != null && currentlyFelling.contains(block);
    }

    public static final Set<Material> EXCLUDED_AUTO_FEED_FOODS = Set.of(
            Material.ROTTEN_FLESH,
            Material.POISONOUS_POTATO,
            Material.PUFFERFISH,
            Material.SPIDER_EYE,
            Material.CHORUS_FRUIT,
            Material.SUSPICIOUS_STEW,
            Material.GOLDEN_APPLE,
            Material.ENCHANTED_GOLDEN_APPLE
    );

    public static final NamespacedKey PHOENIX_COOLDOWN_KEY = new NamespacedKey("smptools", "phoenix_cooldown");

    public ArtifactListener(SMPTools plugin, ArtifactManager artifactManager) {
        this.plugin = plugin;
        this.artifactManager = artifactManager;
        this.bootsFlightKey = new NamespacedKey(plugin, "boots_flight_granted");
        startPassiveTasks();
    }

    private long getCooldown(UUID uuid, ArtifactType type) {
        Map<ArtifactType, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return 0L;
        return playerCooldowns.getOrDefault(type, 0L);
    }

    private void setCooldown(UUID uuid, ArtifactType type, long time) {
        cooldowns.computeIfAbsent(uuid, k -> new EnumMap<>(ArtifactType.class)).put(type, time);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getPersistentDataContainer().has(bootsFlightKey, PersistentDataType.BYTE)) {
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                if (!artifactManager.hasEquippedArtifact(player, ArtifactType.LEAP_FROG_BOOTS)) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.getPersistentDataContainer().remove(bootsFlightKey);
                    bootsFlightGranted.remove(player.getUniqueId());
                } else {
                    bootsFlightGranted.add(player.getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (bootsFlightGranted.remove(uuid) || player.getPersistentDataContainer().has(bootsFlightKey, PersistentDataType.BYTE)) {
            player.getPersistentDataContainer().remove(bootsFlightKey);
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
        cooldowns.remove(uuid);
        fallImmune.remove(uuid);
        homingCompassTargetMode.remove(uuid);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        ArtifactType type = artifactManager.getArtifactType(main);

        if (type == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            long now = System.currentTimeMillis();
            long last = getCooldown(player.getUniqueId(), type);

            switch (type) {
                case WIND_DASH_FEATHER:
                    if (now - last < 5000) {
                        player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Wind Dash on cooldown!</red>"));
                        return;
                    }
                    setCooldown(player.getUniqueId(), type, now);
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
                    Location safeDest = findSafeShadowStepDestination(player, 8);
                    if (safeDest == null) {
                        player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Cannot Shadow Step into obstructed area!</red>"));
                        return;
                    }
                    setCooldown(player.getUniqueId(), type, now);
                    player.teleport(safeDest);
                    player.getWorld().spawnParticle(Particle.PORTAL, safeDest, 50, 0.5, 1.0, 0.5, 0.2);
                    player.getWorld().playSound(safeDest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    event.setCancelled(true);
                    break;

                case PORTABLE_WORKBENCH:
                    player.openWorkbench(null, true);
                    player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 1.0f, 1.2f);
                    event.setCancelled(true);
                    break;

                case HOMING_COMPASS:
                    if (player.isSneaking()) {
                        int mode = (homingCompassTargetMode.getOrDefault(player.getUniqueId(), 0) + 1) % 3;
                        homingCompassTargetMode.put(player.getUniqueId(), mode);
                        String modeName = mode == 0 ? "Nearest Player" : (mode == 1 ? "Nearest Boss" : "Death Location / Grave");
                        player.sendActionBar(MiniMessage.miniMessage().deserialize("<gold>🧭 Homing Compass Target: <yellow>" + modeName + "</yellow></gold>"));
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.4f);
                        event.setCancelled(true);
                    }
                    break;

                case VAMPIRIC_SCYTHE:
                    if (now - last < 20000) {
                        player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Vampiric Scythe on cooldown!</red>"));
                        return;
                    }
                    setCooldown(player.getUniqueId(), type, now);
                    double drained = 0;
                    for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                        if (entity instanceof LivingEntity target && target != player && isHostileTarget(target)) {
                            if (player.hasLineOfSight(target)) {
                                double damage = target.getHealth() * 0.15;
                                target.damage(damage, player);
                                drained += damage;
                                target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, target.getLocation(), 20, 0.3, 0.5, 0.3, 0.1);
                            }
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
                    setCooldown(player.getUniqueId(), type, now);
                    Vector dir = player.getLocation().getDirection().normalize();
                    for (Entity entity : player.getNearbyEntities(8, 8, 8)) {
                        if (entity instanceof LivingEntity target && target != player && isHostileTarget(target)) {
                            if (player.hasLineOfSight(target)) {
                                target.setVelocity(dir.clone().multiply(2.5).setY(0.5));
                                target.damage(6.0, player);
                            }
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
                    setCooldown(player.getUniqueId(), type, now);
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.isCancelled()) return;

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
                event.getHook().setWaitTime(Math.max(20, event.getHook().getWaitTime() / 2));
            } else if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                // 25% chance for double catch bonus
                if (random.nextDouble() < 0.25 && event.getCaught() instanceof Item caughtItem) {
                    ItemStack clone = caughtItem.getItemStack().clone();
                    player.getWorld().dropItemNaturally(caughtItem.getLocation(), clone);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.8f);
                    player.sendActionBar(MiniMessage.miniMessage().deserialize("<gold>🎣 <b>Master Angler Bonus: Double Catch!</b></gold>"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (event.isCancelled()) return;

        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL && fallImmune.contains(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            // Void Saver Charm Check on void damage or lethal lava damage
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID || 
               (event.getCause() == EntityDamageEvent.DamageCause.LAVA && player.getHealth() - event.getFinalDamage() <= 0)) {
                if (artifactManager.hasEquippedArtifact(player, ArtifactType.VOID_SAVER_CHARM)) {
                    event.setCancelled(true);
                    Location safe = player.getWorld().getSpawnLocation();
                    player.teleport(safe);
                    player.setFireTicks(0);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 400, 2));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                    player.getWorld().spawnParticle(Particle.PORTAL, safe, 40, 0.5, 1.0, 0.5, 0.1);
                    player.getWorld().playSound(safe, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                    player.sendActionBar(MiniMessage.miniMessage().deserialize("<purple><b>🔮 Void Saver Charm Teleported You to Safety!</b></purple>"));
                    return;
                }
            }

            // Phoenix Feather Check (exclude VOID damage since Phoenix Feather does not teleport player out of void)
            if (event.getCause() != EntityDamageEvent.DamageCause.VOID && player.getHealth() - event.getFinalDamage() <= 0) {
                if (artifactManager.hasEquippedArtifact(player, ArtifactType.PHOENIX_FEATHER)) {
                    long now = System.currentTimeMillis();
                    long last = getCooldown(player.getUniqueId(), ArtifactType.PHOENIX_FEATHER);
                    if (player.getPersistentDataContainer().has(PHOENIX_COOLDOWN_KEY, PersistentDataType.LONG)) {
                        Long pdcTime = player.getPersistentDataContainer().get(PHOENIX_COOLDOWN_KEY, PersistentDataType.LONG);
                        if (pdcTime != null && pdcTime > last) {
                            last = pdcTime;
                        }
                    }
                    if (now - last >= 60000L) {
                        setCooldown(player.getUniqueId(), ArtifactType.PHOENIX_FEATHER, now);
                        player.getPersistentDataContainer().set(PHOENIX_COOLDOWN_KEY, PersistentDataType.LONG, now);
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
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (artifactManager.hasEquippedArtifact(player, ArtifactType.ALCHEMISTS_SATCHEL)) {
            if (item.getItemMeta() instanceof PotionMeta potionMeta) {
                List<PotionEffect> effectsToDouble = new ArrayList<>(potionMeta.getCustomEffects());
                if (potionMeta.getBasePotionType() != null) {
                    effectsToDouble.addAll(potionMeta.getBasePotionType().getPotionEffects());
                }

                if (!effectsToDouble.isEmpty()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (PotionEffect potionEffect : effectsToDouble) {
                                PotionEffect existing = player.getPotionEffect(potionEffect.getType());
                                if (existing != null) {
                                    player.addPotionEffect(new PotionEffect(
                                            existing.getType(),
                                            existing.getDuration() * 2,
                                            existing.getAmplifier(),
                                            existing.isAmbient(),
                                            existing.hasParticles(),
                                            existing.hasIcon()
                                    ), true);
                                }
                            }
                            player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.2f);
                            player.sendActionBar(MiniMessage.miniMessage().deserialize("<green>🧪 <b>Alchemist's Satchel Doubled Potion Duration!</b></green>"));
                        }
                    }.runTaskLater(plugin, 2L);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (currentlyFelling.contains(event.getBlock())) return;

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
        visited.add(start);

        int maxLogs = 64;
        int count = 0;

        while (!queue.isEmpty() && count < maxLogs) {
            Block current = queue.poll();

            if (!current.equals(start) && current.getType().name().contains("LOG")) {
                currentlyFelling.add(current);
                try {
                    BlockBreakEvent subEvent = new BlockBreakEvent(current, player);
                    Bukkit.getPluginManager().callEvent(subEvent);
                    if (subEvent.isCancelled()) {
                        continue;
                    }
                    current.breakNaturally(tool);
                    count++;
                } finally {
                    currentlyFelling.remove(current);
                }
            }

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block neighbor = current.getRelative(x, y, z);
                        if (!visited.contains(neighbor) && neighbor.getType().name().contains("LOG")) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        if (count > 0 && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
            if (tool != null && tool.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) {
                int unbreaking = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING);
                int damageToApply = 0;
                for (int i = 0; i < count; i++) {
                    if (unbreaking <= 0 || random.nextInt(unbreaking + 1) == 0) {
                        damageToApply++;
                    }
                }
                int newDamage = damageable.getDamage() + damageToApply;
                if (newDamage >= tool.getType().getMaxDurability()) {
                    player.getInventory().setItemInMainHand(null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                } else {
                    damageable.setDamage(newDamage);
                    tool.setItemMeta(damageable);
                }
            }
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (artifactManager.hasEquippedArtifact(player, ArtifactType.LEAP_FROG_BOOTS)) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setFlying(false);
            bootsFlightGranted.remove(player.getUniqueId());
            player.getPersistentDataContainer().remove(bootsFlightKey);

            Vector dir = player.getLocation().getDirection().normalize().multiply(0.8).setY(0.9);
            player.setVelocity(dir);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.4, 0.2, 0.4, 0.05);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SLIME_JUMP, 1.0f, 1.2f);
            grantFallImmunity(player, 6);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Leap Frog Boots flight enable/disable scoped check
        if (artifactManager.hasEquippedArtifact(player, ArtifactType.LEAP_FROG_BOOTS)) {
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                if (!player.getAllowFlight()) {
                    if (player.getLocation().subtract(0, 0.1, 0).getBlock().getType().isSolid()) {
                        player.setAllowFlight(true);
                        bootsFlightGranted.add(player.getUniqueId());
                        player.getPersistentDataContainer().set(bootsFlightKey, PersistentDataType.BYTE, (byte) 1);
                    }
                }
            }
        } else if (bootsFlightGranted.remove(player.getUniqueId()) || player.getPersistentDataContainer().has(bootsFlightKey, PersistentDataType.BYTE)) {
            player.getPersistentDataContainer().remove(bootsFlightKey);
            bootsFlightGranted.remove(player.getUniqueId());
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }

        // Feather Glider Ring check
        if (artifactManager.hasEquippedArtifact(player, ArtifactType.FEATHER_GLIDER_RING)) {
            if (player.isSneaking() && player.getFallDistance() > 1.5 && !player.isOnGround()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0, false, false));
                if (random.nextDouble() < 0.3) {
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 2, 0.2, 0.1, 0.2, 0.01);
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
        // Ticks passive effects every second (20 ticks)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Abyssal Lantern
                    if (artifactManager.hasEquippedArtifact(player, ArtifactType.ABYSSAL_LANTERN)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 300, 0, false, false));
                    }

                    // Magnet Totem
                    if (artifactManager.hasEquippedArtifact(player, ArtifactType.MAGNET_TOTEM)) {
                        Vector pVec = player.getLocation().toVector();
                        for (Entity entity : player.getNearbyEntities(12, 12, 12)) {
                            if (entity instanceof Item || entity instanceof ExperienceOrb) {
                                Vector diff = pVec.clone().subtract(entity.getLocation().toVector());
                                if (diff.lengthSquared() > 0.001) {
                                    entity.setVelocity(diff.normalize().multiply(0.4));
                                }
                            }
                        }
                    }

                    // Auto-Feeder Satchel
                    if (artifactManager.hasEquippedArtifact(player, ArtifactType.AUTO_FEEDER_SATCHEL) && player.getFoodLevel() < 16) {
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null && item.getType().isEdible() && !EXCLUDED_AUTO_FEED_FOODS.contains(item.getType())) {
                                player.setFoodLevel(20);
                                item.setAmount(item.getAmount() - 1);
                                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.0f);
                                break;
                            }
                        }
                    }

                    // Chlorophyll Band (every 3 seconds)
                    if (ticks % 3 == 0 && artifactManager.hasEquippedArtifact(player, ArtifactType.CHLOROPHYLL_BAND)) {
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

                    // Ore Radar Scanner (optimized 9x9x9 scan with chunk-loaded check)
                    if (ticks % 2 == 0 && artifactManager.hasEquippedArtifact(player, ArtifactType.ORE_RADAR_SCANNER)) {
                        Location loc = player.getLocation();
                        World world = loc.getWorld();
                        if (world != null) {
                            int px = loc.getBlockX();
                            int py = loc.getBlockY();
                            int pz = loc.getBlockZ();
                            boolean oreFound = false;
                            int minDistanceSq = Integer.MAX_VALUE;

                            for (int x = -4; x <= 4; x++) {
                                for (int y = -4; y <= 4; y++) {
                                    for (int z = -4; z <= 4; z++) {
                                        int bx = px + x;
                                        int bz = pz + z;
                                        if (world.isChunkLoaded(bx >> 4, bz >> 4)) {
                                            Block b = world.getBlockAt(bx, py + y, bz);
                                            Material mat = b.getType();
                                            if (mat == Material.DIAMOND_ORE || mat == Material.DEEPSLATE_DIAMOND_ORE ||
                                                    mat == Material.ANCIENT_DEBRIS || mat == Material.EMERALD_ORE ||
                                                    mat == Material.DEEPSLATE_EMERALD_ORE) {
                                                player.spawnParticle(Particle.GLOW, b.getLocation().add(0.5, 0.5, 0.5), 1, 0, 0, 0, 0);
                                                oreFound = true;
                                                int distSq = x * x + y * y + z * z;
                                                if (distSq < minDistanceSq) {
                                                    minDistanceSq = distSq;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (oreFound) {
                                float pitch = 1.0f + (float) Math.max(0.0, 1.0 - (Math.sqrt(minDistanceSq) / 8.0));
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f, pitch);
                            }
                        }
                    }

                    // Homing Compass
                    if (artifactManager.hasEquippedArtifact(player, ArtifactType.HOMING_COMPASS) ||
                            artifactManager.isArtifact(player.getInventory().getItemInMainHand(), ArtifactType.HOMING_COMPASS)) {
                        int mode = homingCompassTargetMode.getOrDefault(player.getUniqueId(), 0);
                        Location targetLoc = null;

                        if (mode == 0) {
                            // Nearest Player (bounded search in 256 block radius)
                            double closestDist = Double.MAX_VALUE;
                            for (Player other : player.getWorld().getPlayers()) {
                                if (!other.getUniqueId().equals(player.getUniqueId()) && other.getGameMode() != GameMode.SPECTATOR) {
                                    double d = player.getLocation().distanceSquared(other.getLocation());
                                    if (d < closestDist && d <= 65536.0) {
                                        closestDist = d;
                                        targetLoc = other.getLocation();
                                    }
                                }
                            }
                        } else if (mode == 1) {
                            // Nearest Boss (bounded search in 128 block radius)
                            double closestDist = Double.MAX_VALUE;
                            for (Entity entity : player.getNearbyEntities(128, 128, 128)) {
                                if (entity instanceof Boss || entity instanceof Warden || entity instanceof Wither || 
                                    entity instanceof EnderDragon || entity instanceof ElderGuardian) {
                                    double d = player.getLocation().distanceSquared(entity.getLocation());
                                    if (d < closestDist) {
                                        closestDist = d;
                                        targetLoc = entity.getLocation();
                                    }
                                }
                            }
                        } else if (mode == 2) {
                            // Last Death Location / Grave
                            targetLoc = player.getLastDeathLocation();
                        }

                        if (targetLoc != null) {
                            player.setCompassTarget(targetLoc);
                        } else {
                            player.setCompassTarget(player.getWorld().getSpawnLocation());
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private Location findSafeShadowStepDestination(Player player, double maxDist) {
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        Location target = player.getLocation().add(dir.clone().multiply(maxDist));

        // 1. If target endpoint is safe, verify contiguous wall thickness does not exceed 2 solid blocks
        if (isSafePlayerStand(target) && getMaxContiguousSolidObstacleThickness(player.getLocation(), target) <= 2) {
            return target;
        }

        // 2. Check vertical adjustments at maxDist if contiguous wall thickness <= 2
        for (int dy : new int[]{1, 2, 3, -1, -2}) {
            Location candidate = target.clone().add(0, dy, 0);
            if (isSafePlayerStand(candidate) && getMaxContiguousSolidObstacleThickness(player.getLocation(), candidate) <= 2) {
                return candidate;
            }
        }

        // 3. If obstacle is thicker than 2 solid blocks or target is solid, trace backward along the ray
        // to find the nearest safe spot before the obstacle
        for (double d = maxDist - 0.5; d >= 1.0; d -= 0.5) {
            Location fallback = player.getLocation().add(dir.clone().multiply(d));
            if (isSafePlayerStand(fallback) && getMaxContiguousSolidObstacleThickness(player.getLocation(), fallback) == 0) {
                return fallback;
            }
        }

        return null;
    }

    private int getMaxContiguousSolidObstacleThickness(Location from, Location to) {
        if (from == null || to == null || from.getWorld() == null) return Integer.MAX_VALUE;
        double dist = from.distance(to);
        if (dist <= 0.0) return 0;
        Vector step = to.toVector().subtract(from.toVector()).normalize().multiply(0.2);
        int maxContiguous = 0;
        int currentContiguous = 0;
        Block lastFeet = null;
        Block lastHead = null;

        for (double d = 0.2; d < dist - 0.1; d += 0.2) {
            Location sample = from.clone().add(step.clone().multiply(d / 0.2));
            Block feet = sample.getBlock();
            Block head = sample.clone().add(0, 1, 0).getBlock();

            boolean solidFeet = feet.getType().isSolid();
            boolean solidHead = head.getType().isSolid();

            if (solidFeet || solidHead) {
                if (!feet.equals(lastFeet) || !head.equals(lastHead)) {
                    currentContiguous++;
                    lastFeet = feet;
                    lastHead = head;
                    if (currentContiguous > maxContiguous) {
                        maxContiguous = currentContiguous;
                    }
                }
            } else {
                currentContiguous = 0;
                lastFeet = null;
                lastHead = null;
            }
        }
        return maxContiguous;
    }

    private boolean isSafePlayerStand(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();

        if (feet.getType().isSolid() || head.getType().isSolid()) {
            return false;
        }
        if (feet.isLiquid()) {
            return false;
        }
        return true;
    }

    private boolean isHostileTarget(LivingEntity target) {
        if (target == null || target.isDead()) return false;
        if (target instanceof Player) return false;
        if (target instanceof org.bukkit.entity.Tameable tameable && tameable.isTamed()) return false;
        if (target instanceof org.bukkit.entity.Villager || target instanceof org.bukkit.entity.WanderingTrader) return false;
        if (target instanceof org.bukkit.entity.ArmorStand) return false;

        return target instanceof org.bukkit.entity.Enemy;
    }
}
