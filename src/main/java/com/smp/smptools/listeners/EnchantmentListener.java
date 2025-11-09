package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.enchants.TelekinesisEnchant;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class EnchantmentListener implements Listener {

    private final SMPTools plugin;

    public EnchantmentListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (plugin.getEnchantmentManager().hasEnchantment(tool, new TelekinesisEnchant())) {
            event.setCancelled(true); // Cancel the drop
            for (org.bukkit.entity.Item item : event.getItems()) {
                event.getPlayer().getInventory().addItem(item.getItemStack());
            }
        }
    }
}
