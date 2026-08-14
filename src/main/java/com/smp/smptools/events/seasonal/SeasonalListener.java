package com.smp.smptools.events.seasonal;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Handles in-world discovery of hidden seasonal targets, protection of targets, Trick-or-Treating, and Summer buffs.
 */
public class SeasonalListener implements Listener {

    private final SMPTools plugin;
    private final SeasonalManager seasonalManager;
    private final Random random = new Random();
    private final Map<UUID, Long> trickOrTreatCooldown = new HashMap<>();

    public SeasonalListener(SMPTools plugin, SeasonalManager seasonalManager) {
        this.plugin = plugin;
        this.seasonalManager = seasonalManager;
        startSummerPassiveTask();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Location loc = event.getClickedBlock().getLocation();
        Player player = event.getPlayer();

        // Check Halloween Pumpkin hit
        Integer pumpkinId = seasonalManager.getPumpkinIdAt(loc);
        if (pumpkinId != null) {
            seasonalManager.discoverPumpkin(player, pumpkinId);
            event.setCancelled(true);
            return;
        }

        // Check Easter Egg hit
        Integer eggId = seasonalManager.getEggIdAt(loc);
        if (eggId != null) {
            seasonalManager.discoverEgg(player, eggId);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTargetBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        Integer pumpkinId = seasonalManager.getPumpkinIdAt(loc);
        Integer eggId = seasonalManager.getEggIdAt(loc);

        if (pumpkinId != null || eggId != null) {
            Player player = event.getPlayer();
            if (!(player.hasPermission("smptools.seasonal.admin") && player.getGameMode() == GameMode.CREATIVE)) {
                event.setCancelled(true);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot break seasonal scavenger hunt targets!</red>"));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetBlockBreakMonitor(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        Integer pumpkinId = seasonalManager.getPumpkinIdAt(loc);
        Integer eggId = seasonalManager.getEggIdAt(loc);

        if (pumpkinId != null || eggId != null) {
            Player player = event.getPlayer();
            if (player.hasPermission("smptools.seasonal.admin") && player.getGameMode() == GameMode.CREATIVE) {
                if (pumpkinId != null) {
                    seasonalManager.removePumpkinLocation(pumpkinId);
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Removed Pumpkin #" + pumpkinId + " from registered seasonal locations.</yellow>"));
                }
                if (eggId != null) {
                    seasonalManager.removeEggLocation(eggId);
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Removed Easter Egg #" + eggId + " from registered seasonal locations.</yellow>"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> seasonalManager.getPumpkinIdAt(b.getLocation()) != null || seasonalManager.getEggIdAt(b.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> seasonalManager.getPumpkinIdAt(b.getLocation()) != null || seasonalManager.getEggIdAt(b.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (seasonalManager.getPumpkinIdAt(b.getLocation()) != null || seasonalManager.getEggIdAt(b.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (seasonalManager.getPumpkinIdAt(b.getLocation()) != null || seasonalManager.getEggIdAt(b.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (seasonalManager.getPumpkinIdAt(event.getBlock().getLocation()) != null || seasonalManager.getEggIdAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (seasonalManager.getPumpkinIdAt(event.getBlock().getLocation()) != null || seasonalManager.getEggIdAt(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;

        Player player = event.getPlayer();
        if (seasonalManager.getCurrentSeason() != SeasonType.HALLOWEEN) return;
        if (!plugin.getSeasonalConfig().getBoolean("seasonal.halloween.trick_or_treat_enabled", true)) return;

        // Check if player is wearing a helmet or carved pumpkin
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType().isAir()) return;

        long now = System.currentTimeMillis();
        trickOrTreatCooldown.entrySet().removeIf(e -> now - e.getValue() > 60000);
        long last = trickOrTreatCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 60000) { // 1 min cooldown per player
            return;
        }
        trickOrTreatCooldown.put(player.getUniqueId(), now);

        if (random.nextDouble() < 0.70) {
            // Treat!
            ItemStack cookies = new ItemStack(Material.COOKIE, 4);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(cookies);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gold>🍬 <b>Trick or Treat!</b> The villager gave you Halloween treats!</gold>"));
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, event.getRightClicked().getLocation().add(0, 1.5, 0), 10);
        } else {
            // Trick!
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>🦇 <b>Trick!</b> The villager was startled and a bat flew out!</red>"));
            event.getRightClicked().getWorld().spawnEntity(event.getRightClicked().getLocation().add(0, 1, 0), EntityType.BAT);
        }
    }

    private void startSummerPassiveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (seasonalManager.getCurrentSeason() != SeasonType.SUMMER) return;
                if (!plugin.getSeasonalConfig().getBoolean("seasonal.summer.solar_flares_enabled", true)) return;
                if (!plugin.getSeasonalConfig().getBoolean("seasonal.summer.solar_flare_buffs", true)) return;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    World world = player.getWorld();
                    long time = world.getTime();

                    // Midday (4000 to 8000 ticks) and open sky
                    if (time >= 4000 && time <= 8000 && world.getHighestBlockYAt(player.getLocation()) <= player.getLocation().getBlockY()) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, 1, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, false, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 60L, 60L);
    }
}
