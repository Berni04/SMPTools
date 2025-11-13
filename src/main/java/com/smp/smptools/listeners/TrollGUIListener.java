package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap; // Import HashMap
import java.util.Map; // Import Map
import org.bukkit.Location; // Import Location
import org.bukkit.Particle; // Import Particle

public class TrollGUIListener implements Listener {

    private final SMPTools plugin;
    private static final String GUI_TITLE = "Troll Menu";
    private static final Random random = new Random();
    private static final Map<Integer, String> trollIdMap = new HashMap<>(); // Map to store hash code to trollId mapping

    public TrollGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    public static void openTrollGUI(Player opener, Player target) {
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE + " - " + target.getName());

        // Troll Options (10-15 items)
        // Row 1
        gui.setItem(0, createTrollItem(Material.PAPER, "<red>Fake OP</red>", "<gray>Sends a fake OP message to " + target.getName() + "</gray>", "fake_op"));
        gui.setItem(1, createTrollItem(Material.BARRIER, "<red>Fake Ban</red>", "<gray>Sends a fake ban message to " + target.getName() + "</gray>", "fake_ban"));
        gui.setItem(2, createTrollItem(Material.REDSTONE_BLOCK, "<red>Fake Crash</red>", "<gray>Sends a fake client crash message to " + target.getName() + "</gray>", "fake_crash"));
        gui.setItem(3, createTrollItem(Material.CHEST, "<gold>Scramble Inventory</gold>", "<gray>Randomizes " + target.getName() + "'s inventory slots.</gray>", "scramble_inv"));
        gui.setItem(4, createTrollItem(Material.DROPPER, "<gold>Drop Inventory</gold>", "<gray>Forces " + target.getName() + " to drop all items.</gray>", "drop_inv"));
        gui.setItem(5, createTrollItem(Material.COAL, "<dark_gray>Blindness</dark_gray>", "<gray>Applies temporary blindness to " + target.getName() + ".</gray>", "blindness"));
        gui.setItem(6, createTrollItem(Material.ROTTEN_FLESH, "<dark_green>Nausea</dark_green>", "<gray>Applies temporary nausea to " + target.getName() + ".</gray>", "nausea"));
        gui.setItem(7, createTrollItem(Material.SOUL_SAND, "<dark_gray>Slowness</dark_gray>", "<gray>Applies extreme slowness to " + target.getName() + ".</gray>", "slowness"));
        gui.setItem(8, createTrollItem(Material.FEATHER, "<aqua>Levitation</aqua>", "<gray>Applies temporary levitation to " + target.getName() + ".</gray>", "levitation"));

        // Row 2
        gui.setItem(9, createTrollItem(Material.LAVA_BUCKET, "<red>Fake Lava</red>", "<gray>Spawns temporary lava near " + target.getName() + ".</gray>", "fake_lava"));
        gui.setItem(10, createTrollItem(Material.WATER_BUCKET, "<blue>Fake Water</blue>", "<gray>Spawns temporary water near " + target.getName() + ".</gray>", "fake_water"));
        gui.setItem(11, createTrollItem(Material.TRIDENT, "<yellow>Lightning Strike</yellow>", "<gray>Strikes " + target.getName() + " with lightning (no damage).</gray>", "lightning"));
        gui.setItem(12, createTrollItem(Material.TNT, "<red>Safe Explosion</red>", "<gray>Creates a visual explosion near " + target.getName() + " (no damage).</gray>", "safe_explosion"));
        gui.setItem(13, createTrollItem(Material.CRAFTING_TABLE, "<gold>Swap Hotbar</gold>", "<gray>Swaps " + target.getName() + "'s hotbar with inventory row.</gray>", "swap_hotbar"));
        gui.setItem(14, createTrollItem(Material.SKELETON_SKULL, "<red>Fake Death Message</red>", "<gray>Broadcasts a fake death message for " + target.getName() + ".</gray>", "fake_death"));
        gui.setItem(15, createTrollItem(Material.ENDER_PEARL, "<light_purple>Teleport Randomly</light_purple>", "<gray>Teleports " + target.getName() + " to a random nearby location.</gray>", "teleport_random"));
        gui.setItem(16, createTrollItem(Material.KNOWLEDGE_BOOK, "<green>Fake Advancement</green>", "<gray>Grants a fake advancement to " + target.getName() + ".</gray>", "fake_advancement"));
        gui.setItem(17, createTrollItem(Material.JUKEBOX, "<aqua>Sound Spam</aqua>", "<gray>Spams " + target.getName() + " with annoying sounds.</gray>", "sound_spam"));

        // Row 3
        gui.setItem(18, createTrollItem(Material.WRITABLE_BOOK, "<gray>Chat Scramble</gray>", "<gray>Temporarily scrambles " + target.getName() + "'s chat messages.</gray>", "chat_scramble"));
        gui.setItem(19, createTrollItem(Material.ENDER_EYE, "<dark_purple>Force POV</dark_purple>", "<gray>Forces " + target.getName() + "'s perspective.</gray>", "force_pov"));
        gui.setItem(20, createTrollItem(Material.ANVIL, "<dark_gray>Fake Item Break</dark_gray>", "<gray>Sends a message that " + target.getName() + "'s item broke.</gray>", "fake_item_break"));


        // Close button
        gui.setItem(53, createTrollItem(Material.RED_WOOL, "<red>Close</red>", "<gray>Close the troll menu.</gray>", "close"));

        opener.openInventory(gui);
    }

    private static ItemStack createTrollItem(Material material, String name, String lore, String trollId) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name));
        meta.lore(Collections.singletonList(MiniMessage.miniMessage().deserialize(lore)));
        meta.setCustomModelData(trollId.hashCode()); // Use hash code for unique identification
        item.setItemMeta(meta);
        trollIdMap.put(trollId.hashCode(), trollId); // Store mapping
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Get the plain text title of the inventory
        String clickedTitle = event.getView().getTitle();
        
        // Check if it's our Troll Menu
        if (!clickedTitle.startsWith(GUI_TITLE + " - ")) {
            return;
        }

        event.setCancelled(true);

        Player opener = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasCustomModelData()) {
            return;
        }

        String targetName = clickedTitle.substring((GUI_TITLE + " - ").length());
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            opener.sendMessage(MiniMessage.miniMessage().deserialize("<red>Target player is no longer online!</red>"));
            opener.closeInventory();
            return;
        }

        String trollId = String.valueOf(clickedItem.getItemMeta().getCustomModelData()); // Retrieve troll ID

        if (trollId.equals(String.valueOf("close".hashCode()))) {
            opener.closeInventory();
            return;
        }

        // Permission check for specific troll
        String originalTrollId = trollIdMap.get(Integer.parseInt(trollId));
        if (originalTrollId == null || !opener.hasPermission("smptools.troll." + originalTrollId)) {
            opener.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to use this troll option!</red>"));
            return;
        }

        opener.sendMessage(MiniMessage.miniMessage().deserialize("<green>Executing troll '" + MiniMessage.miniMessage().serialize(clickedItem.getItemMeta().displayName()) + "' on " + target.getName() + "!</green>"));
        executeTroll(opener, target, originalTrollId);
        opener.closeInventory();
    }

    private void executeTroll(Player opener, Player target, String trollId) {
        switch (trollId) {
            case "fake_op":
                target.sendMessage(MiniMessage.miniMessage().deserialize("<gray>[Server: Opped " + target.getName() + "]</gray>"));
                break;
            case "fake_ban":
                target.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have been permanently banned from this server!</red>"));
                break;
            case "fake_crash":
                target.sendMessage(MiniMessage.miniMessage().deserialize("<red>Internal Server Error: java.lang.NullPointerException</red>"));
                break;
            case "scramble_inv":
                scrambleInventory(target);
                break;
            case "drop_inv":
                dropInventory(target);
                break;
            case "blindness":
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 10, 1)); // 10 seconds
                break;
            case "nausea":
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 10, 1)); // 10 seconds
                break;
            case "slowness":
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 10, 5)); // 10 seconds, extreme
                break;
            case "levitation":
                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20 * 5, 1)); // 5 seconds
                break;
            case "fake_lava":
                spawnTemporaryBlock(target, Material.LAVA);
                break;
            case "fake_water":
                spawnTemporaryBlock(target, Material.WATER);
                break;
            case "lightning":
                target.getWorld().strikeLightningEffect(target.getLocation()); // Effect only, no damage
                break;
            case "safe_explosion":
                target.playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                break;
            case "swap_hotbar":
                swapHotbar(target);
                break;
            case "fake_death":
                broadcastFakeDeathMessage(target);
                break;
            case "teleport_random":
                teleportRandomly(target);
                break;
            case "fake_advancement":
                sendFakeAdvancement(target);
                break;
            case "sound_spam":
                soundSpam(target);
                break;
            case "chat_scramble":
                // This requires modifying ChatListener or using a temporary chat interceptor
                // For now, we'll just send a message indicating it happened.
                opener.sendMessage(MiniMessage.miniMessage().deserialize("<red>Chat Scramble is not fully implemented yet. (Requires ChatListener modification)</red>"));
                break;
            case "force_pov":
                // This is client-side and not directly controllable via Bukkit API.
                // Can send a message indicating it happened.
                opener.sendMessage(MiniMessage.miniMessage().deserialize("<red>Force POV is not directly controllable via Bukkit API.</red>"));
                break;
            case "fake_item_break":
                target.sendMessage(MiniMessage.miniMessage().deserialize("<red>Your " + MiniMessage.miniMessage().serialize(target.getInventory().getItemInMainHand().displayName()) + " just broke!</red>"));
                break;
            default:
                opener.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown troll option: " + trollId + "</red>"));
                break;
        }
    }

    private void scrambleInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        List<ItemStack> shuffled = new ArrayList<>(List.of(contents));
        Collections.shuffle(shuffled);
        player.getInventory().setContents(shuffled.toArray(new ItemStack[0]));
    }

    private void dropInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
        player.getInventory().clear();
    }

    private void spawnTemporaryBlock(Player player, Material blockType) {
        Block targetBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (targetBlock.getType().isSolid()) {
            Block tempBlock = targetBlock.getRelative(BlockFace.UP);
            Material originalType = tempBlock.getType();
            tempBlock.setType(blockType);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (tempBlock.getType() == blockType) {
                        tempBlock.setType(originalType);
                    }
                }
            }.runTaskLater(plugin, 20 * 3); // 3 seconds
        }
    }

    private void swapHotbar(Player player) {
        ItemStack[] hotbar = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            hotbar[i] = player.getInventory().getItem(i);
        }

        ItemStack[] inventoryRow = new ItemStack[9];
        int randomRow = random.nextInt(3) + 9; // Rows 1-3 (slots 9-35)
        for (int i = 0; i < 9; i++) {
            inventoryRow[i] = player.getInventory().getItem(randomRow + i);
        }

        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, inventoryRow[i]);
            player.getInventory().setItem(randomRow + i, hotbar[i]);
        }
    }

    private void broadcastFakeDeathMessage(Player player) {
        // This is a simplified fake death message. More complex ones would mimic actual death causes.
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<red>" + player.getName() + " was slain by [Troll God]</red>"));
    }

    private void teleportRandomly(Player player) {
        int radius = 10;
        int x = player.getLocation().getBlockX() + random.nextInt(radius * 2) - radius;
        int z = player.getLocation().getBlockZ() + random.nextInt(radius * 2) - radius;
        int y = player.getWorld().getHighestBlockYAt(x, z) + 1; // Teleport to highest block + 1
        player.teleport(new Location(player.getWorld(), x + 0.5, y, z + 0.5));
    }

    private void sendFakeAdvancement(Player player) {
        // This is a client-side packet, not directly available in Bukkit API.
        // For a simple troll, we can send a message that looks like an advancement.
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>" + player.getName() + " has made the advancement [Troll Master]</green>"));
    }

    private void soundSpam(Player player) {
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 5) { // Spam 5 times
                    this.cancel();
                    return;
                }
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 1.0f, 1.0f);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 10L); // Every 0.5 seconds
    }
}
