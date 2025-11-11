package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDismountEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SitListener implements Listener {

    private final SMPTools plugin;
    private final Set<UUID> sittingPlayers = new HashSet<>();

    public SitListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("features.sit-on-stairs.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getBlockData() instanceof Stairs)) {
            return;
        }

        Stairs stairs = (Stairs) block.getBlockData();
        if (stairs.getHalf() == Bisected.Half.TOP) {
            return; // Can't sit on upside-down stairs
        }

        // Check if block above is solid
        if (block.getRelative(0, 1, 0).getType().isSolid()) {
            return;
        }

        // Player must have an empty hand
        if (player.getInventory().getItemInMainHand().getType() != org.bukkit.Material.AIR) {
            return;
        }

        // Prevent sitting while sneaking
        if (player.isSneaking()) {
            return;
        }

        Location sitLocation = block.getLocation().add(0.5, 1.2, 0.5);
        ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(sitLocation, EntityType.ARMOR_STAND);

        armorStand.setGravity(false);
        armorStand.setVisible(false);
        armorStand.setSmall(true);
        armorStand.setMarker(true);
        armorStand.addPassenger(player);

        sittingPlayers.add(player.getUniqueId());
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (sittingPlayers.contains(player.getUniqueId())) {
            if (event.getDismounted() instanceof ArmorStand) {
                ArmorStand armorStand = (ArmorStand) event.getDismounted();
                if (armorStand.getPassengers().contains(player)) {
                    armorStand.remove();
                    sittingPlayers.remove(player.getUniqueId());
                }
            }
        }
    }
}
