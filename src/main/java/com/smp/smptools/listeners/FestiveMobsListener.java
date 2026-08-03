package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.Random;

public class FestiveMobsListener implements Listener {

    private final SMPTools plugin;
    private final Random random = new Random();
    private FileConfiguration christmasConfig;

    public FestiveMobsListener(SMPTools plugin) {
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
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (christmasConfig == null || !christmasConfig.getBoolean("festive-mobs.enabled"))
            return;

        if (event.getEntity() instanceof Zombie) {
            Zombie zombie = (Zombie) event.getEntity();
            ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
            LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
            meta.setColor(Color.RED);
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize(SMPTools.getInstance().getMessageManager().getRawMessage("christmas.santa-hat")));
            helmet.setItemMeta(meta);
            zombie.getEquipment().setHelmet(helmet);
        } else if (event.getEntity() instanceof Skeleton) {
            Skeleton skeleton = (Skeleton) event.getEntity();
            ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
            LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
            meta.setColor(Color.GREEN);
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize(SMPTools.getInstance().getMessageManager().getRawMessage("christmas.elf-hat")));
            helmet.setItemMeta(meta);
            skeleton.getEquipment().setHelmet(helmet);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (christmasConfig == null || !christmasConfig.getBoolean("festive-mobs.enabled"))
            return;

        LivingEntity entity = event.getEntity();
        if (entity instanceof Zombie || entity instanceof Skeleton || entity instanceof Creeper) {
            double chance = christmasConfig.getDouble("festive-mobs.candy-cane-chance", 0.05);
            if (random.nextDouble() < chance) {
                ItemStack candyCane = new ItemStack(Material.SUGAR);
                net.kyori.adventure.text.Component name = Component.text("Candy Cane", NamedTextColor.RED);
                candyCane.getItemMeta().displayName(name); // Note: This won't work directly, need to set meta back

                // Fix meta setting
                org.bukkit.inventory.meta.ItemMeta meta = candyCane.getItemMeta();
                meta.displayName(name);
                meta.lore(java.util.List.of(Component.text("Sweet and speedy!", NamedTextColor.GRAY)));
                candyCane.setItemMeta(meta);

                entity.getWorld().dropItemNaturally(entity.getLocation(), candyCane);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (christmasConfig == null || !christmasConfig.getBoolean("festive-mobs.enabled"))
            return;
        if (!christmasConfig.getBoolean("festive-mobs.creeper-confetti"))
            return;

        if (event.getEntity() instanceof Creeper) {
            event.blockList().clear(); // Prevent block damage

            // Spawn confetti (firework)
            Firework firework = (Firework) event.getLocation().getWorld().spawnEntity(event.getLocation(),
                    EntityType.FIREWORK_ROCKET);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BURST)
                    .withColor(Color.RED, Color.GREEN, Color.WHITE)
                    .withFade(Color.YELLOW)
                    .flicker(true)
                    .build());
            meta.setPower(0);
            firework.setFireworkMeta(meta);
            firework.detonate();
        }
    }

    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (christmasConfig == null || !christmasConfig.getBoolean("festive-mobs.enabled"))
            return;

        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.SUGAR && item.hasItemMeta()) {
                Component displayName = item.getItemMeta().displayName();
                if (displayName != null
                        && "Candy Cane".equals(((net.kyori.adventure.text.TextComponent) displayName).content())) {
                    event.setCancelled(true); // Prevent normal interaction if any

                    // Consume item
                    item.setAmount(item.getAmount() - 1);

                    // Apply effects
                    event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1)); // Speed II for
                                                                                                         // 10 seconds
                    event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(),
                            org.bukkit.Sound.ENTITY_PLAYER_BURP, 1f, 1f);
                    event.getPlayer().sendMessage(SMPTools.getInstance().getMessageManager().getMessage("christmas.sweet"));
                }
            }
        }
    }
}
