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
import java.util.Arrays; // Import Arrays
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location; // Import Location
import org.bukkit.Particle; // Import Particle

public class TrollGUIListener implements Listener {

    private final SMPTools plugin;
    private static final String GUI_TITLE = "Troll Menu";
    private static final Random random = new Random();
    private static final Map<Integer, String> trollIdMap = new ConcurrentHashMap<>();
    private static final Map<java.util.UUID, Long> scrambledPlayers = new ConcurrentHashMap<>();

    static {
        trollIdMap.put(1, "fake_op");
        trollIdMap.put(2, "fake_ban");
        trollIdMap.put(3, "fake_crash");
        trollIdMap.put(4, "scramble_inv");
        trollIdMap.put(5, "drop_inv");
        trollIdMap.put(6, "blindness");
        trollIdMap.put(7, "nausea");
        trollIdMap.put(8, "slowness");
        trollIdMap.put(9, "levitation");
        trollIdMap.put(10, "fake_lava");
        trollIdMap.put(11, "fake_water");
        trollIdMap.put(12, "lightning");
        trollIdMap.put(13, "safe_explosion");
        trollIdMap.put(14, "swap_hotbar");
        trollIdMap.put(15, "fake_death");
        trollIdMap.put(16, "teleport_random");
        trollIdMap.put(17, "fake_advancement");
        trollIdMap.put(18, "sound_spam");
        trollIdMap.put(19, "chat_scramble");
        trollIdMap.put(21, "fake_item_break");
        trollIdMap.put(22, "fake_lag_message");
        trollIdMap.put(23, "fake_join_leave");
        trollIdMap.put(24, "random_item_rename");
        trollIdMap.put(25, "temp_block_replace");
        trollIdMap.put(26, "sound_loop");
        trollIdMap.put(99, "close");
    }

    public TrollGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    public static boolean isChatScrambled(Player player) {
        if (scrambledPlayers.containsKey(player.getUniqueId())) {
            if (System.currentTimeMillis() < scrambledPlayers.get(player.getUniqueId())) {
                return true;
            } else {
                scrambledPlayers.remove(player.getUniqueId());
            }
        }
        return false;
    }

    public static void openTrollGUI(Player opener, Player target) {
        com.smp.smptools.gui.GuiHolder holder = new com.smp.smptools.gui.GuiHolder(com.smp.smptools.gui.GuiHolder.MenuType.TROLL, opener.getUniqueId());
        Inventory gui = Bukkit.createInventory(holder, 54, GUI_TITLE + " - " + target.getName());
        holder.setInventory(gui);

        // Troll Options (10-15 items)
        // Row 1
        gui.setItem(0, createTrollItem(Material.PAPER, "<red>Fake OP</red>",
                "<gray>Sends a fake OP message to " + target.getName() + "</gray>", 1));
        gui.setItem(1, createTrollItem(Material.BARRIER, "<red>Fake Ban</red>",
                "<gray>Sends a fake ban message to " + target.getName() + "</gray>", 2));
        gui.setItem(2, createTrollItem(Material.REDSTONE_BLOCK, "<red>Fake Crash</red>",
                "<gray>Sends a fake client crash message to " + target.getName() + "</gray>", 3));
        gui.setItem(3, createTrollItem(Material.CHEST, "<gold>Scramble Inventory</gold>",
                "<gray>Randomizes " + target.getName() + "'s inventory slots.</gray>", 4));
        gui.setItem(4, createTrollItem(Material.DROPPER, "<gold>Drop Inventory</gold>",
                "<gray>Forces " + target.getName() + " to drop all items.</gray>", 5));
        gui.setItem(5, createTrollItem(Material.COAL, "<dark_gray>Blindness</dark_gray>",
                "<gray>Applies temporary blindness to " + target.getName() + ".</gray>", 6));
        gui.setItem(6, createTrollItem(Material.ROTTEN_FLESH, "<dark_green>Nausea</dark_green>",
                "<gray>Applies temporary nausea to " + target.getName() + ".</gray>", 7));
        gui.setItem(7, createTrollItem(Material.SOUL_SAND, "<dark_gray>Slowness</dark_gray>",
                "<gray>Applies extreme slowness to " + target.getName() + ".</gray>", 8));
        gui.setItem(8, createTrollItem(Material.FEATHER, "<aqua>Levitation</aqua>",
                "<gray>Applies temporary levitation to " + target.getName() + ".</gray>", 9));

        // Row 2
        gui.setItem(9, createTrollItem(Material.LAVA_BUCKET, "<red>Fake Lava</red>",
                "<gray>Spawns temporary lava near " + target.getName() + ".</gray>", 10));
        gui.setItem(10, createTrollItem(Material.WATER_BUCKET, "<blue>Fake Water</blue>",
                "<gray>Spawns temporary water near " + target.getName() + ".</gray>", 11));
        gui.setItem(11, createTrollItem(Material.TRIDENT, "<yellow>Lightning Strike</yellow>",
                "<gray>Strikes " + target.getName() + " with lightning (no damage).</gray>", 12));
        gui.setItem(12, createTrollItem(Material.TNT, "<red>Safe Explosion</red>",
                "<gray>Creates a visual explosion near " + target.getName() + " (no damage).</gray>", 13));
        gui.setItem(13, createTrollItem(Material.CRAFTING_TABLE, "<gold>Swap Hotbar</gold>",
                "<gray>Swaps " + target.getName() + "'s hotbar with inventory row.</gray>", 14));
        gui.setItem(14, createTrollItem(Material.SKELETON_SKULL, "<red>Fake Death Message</red>",
                "<gray>Broadcasts a fake death message for " + target.getName() + ".</gray>", 15));
        gui.setItem(15, createTrollItem(Material.ENDER_PEARL, "<light_purple>Teleport Randomly</light_purple>",
                "<gray>Teleports " + target.getName() + " to a random nearby location.</gray>", 16));
        gui.setItem(16, createTrollItem(Material.KNOWLEDGE_BOOK, "<green>Fake Advancement</green>",
                "<gray>Grants a fake advancement to " + target.getName() + ".</gray>", 17));
        gui.setItem(17, createTrollItem(Material.JUKEBOX, "<aqua>Sound Spam</aqua>",
                "<gray>Spams " + target.getName() + " with annoying sounds.</gray>", 18));

        // Row 3
        gui.setItem(18, createTrollItem(Material.WRITABLE_BOOK, "<gray>Chat Scramble</gray>",
                "<gray>Temporarily scrambles " + target.getName() + "'s chat messages.</gray>", 19));
        gui.setItem(19, createTrollItem(Material.CLOCK, "<aqua>Fake Lag Message</aqua>",
                "<gray>Sends a repeating 'Lag detected!' message to " + target.getName() + ".</gray>", 22));
        gui.setItem(20, createTrollItem(Material.OAK_DOOR, "<green>Fake Join/Leave</green>",
                "<gray>Broadcasts a fake join/leave message for a random player.</gray>", 23));
        gui.setItem(21, createTrollItem(Material.NAME_TAG, "<light_purple>Random Item Rename</light_purple>",
                "<gray>Renames a random item in " + target.getName() + "'s inventory.</gray>", 24));
        gui.setItem(22, createTrollItem(Material.SPONGE, "<yellow>Temporary Block Replace</yellow>",
                "<gray>Temporarily replaces blocks around " + target.getName() + ".</gray>", 25));
        gui.setItem(23, createTrollItem(Material.NOTE_BLOCK, "<red>Sound Loop</red>",
                "<gray>Plays an annoying sound on loop for " + target.getName() + ".</gray>", 26));
        gui.setItem(24, createTrollItem(Material.ANVIL, "<dark_gray>Fake Item Break</dark_gray>",
                "<gray>Sends a message that " + target.getName() + "'s item broke.</gray>", 21));

        // Close button
        gui.setItem(53,
                createTrollItem(Material.RED_WOOL, "<red>Close</red>", "<gray>Close the troll menu.</gray>", 99));

        opener.openInventory(gui);
    }

    private static ItemStack createTrollItem(Material material, String name, String lore, int trollId) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name));
        meta.lore(Collections.singletonList(MiniMessage.miniMessage().deserialize(lore)));
        meta.setCustomModelData(trollId); // Use explicit integer ID for unique identification
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof com.smp.smptools.gui.GuiHolder holder) ||
                holder.getType() != com.smp.smptools.gui.GuiHolder.MenuType.TROLL) {
            return;
        }

        event.setCancelled(true);

        Player opener = (Player) event.getWhoClicked();
        if (!opener.hasPermission("smptools.troll")) {
            opener.closeInventory();
            return;
        }

        String clickedTitle = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasCustomModelData()) {
            return;
        }

        String targetName = clickedTitle.substring((GUI_TITLE + " - ").length());
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            opener.sendMessage(plugin.getMessageManager().getMessage("troll.target-offline", opener));
            opener.closeInventory();
            return;
        }

        int trollId = clickedItem.getItemMeta().getCustomModelData(); // Retrieve explicit integer ID

        if (trollId == 99) { // Check for close button ID
            opener.closeInventory();
            return;
        }

        String originalTrollId = trollIdMap.get(trollId); // Use explicit ID directly for lookup
        if (originalTrollId == null) { // Should not happen if trollIdMap is correctly populated
            opener.sendMessage(plugin.getMessageManager().getMessage("troll.unknown-option-id", opener,
                    Map.of("id", String.valueOf(trollId))));
            opener.closeInventory();
            return;
        }

        if (!target.isOnline() || !target.isValid()) {
            opener.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
            opener.closeInventory();
            return;
        }

        String trollName = MiniMessage.miniMessage().serialize(clickedItem.getItemMeta().displayName());
        opener.sendMessage(plugin.getMessageManager().getMessage("troll.executing", opener,
                Map.of("troll", trollName, "target", target.getName())));
        executeTroll(opener, target, originalTrollId);
        opener.closeInventory();
    }

    private void executeTroll(Player opener, Player target, String trollId) {
        switch (trollId) {
            case "fake_op":
                target.sendMessage(plugin.getMessageManager().getMessage("troll.fake-op", target,
                        Map.of("name", target.getName())));
                break;
            case "fake_ban":
                target.sendMessage(plugin.getMessageManager().getMessage("troll.fake-ban", target));
                break;
            case "fake_crash":
                target.sendMessage(plugin.getMessageManager().getMessage("troll.fake-crash", target));
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
                scrambledPlayers.put(target.getUniqueId(), System.currentTimeMillis() + 30000); // 30 seconds
                opener.sendMessage(plugin.getMessageManager().getMessage("troll.chat-scramble-activated", opener,
                        Map.of("name", target.getName())));
                break;
            case "fake_lag_message":
                fakeLagMessage(target);
                break;
            case "fake_join_leave":
                fakeJoinLeave(target);
                break;
            case "random_item_rename":
                randomItemRename(target);
                break;
            case "temp_block_replace":
                temporaryBlockReplace(target);
                break;
            case "sound_loop":
                soundLoop(target);
                break;
            case "fake_item_break":
                String itemName = MiniMessage.miniMessage().serialize(
                        target.getInventory().getItemInMainHand().displayName());
                target.sendMessage(plugin.getMessageManager().getMessage("troll.item-broke", target,
                        Map.of("item", itemName)));
                break;
            default:
                opener.sendMessage(plugin.getMessageManager().getMessage("troll.unknown-option", opener,
                        Map.of("id", String.valueOf(trollId))));
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
        if (!player.isOnline()) return;
        Block targetBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (targetBlock.getType().isSolid()) {
            Block tempBlock = targetBlock.getRelative(BlockFace.UP);
            if (!tempBlock.getType().isAir()) return;
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
        // This is a simplified fake death message. More complex ones would mimic actual
        // death causes.
        Bukkit.broadcast(plugin.getMessageManager().getMessage("troll.fake-death", null,
                Map.of("name", player.getName())));
    }

    private void teleportRandomly(Player player) {
        if (!player.isOnline()) return;
        int radius = 10;
        int x = player.getLocation().getBlockX() + random.nextInt(radius * 2) - radius;
        int z = player.getLocation().getBlockZ() + random.nextInt(radius * 2) - radius;
        Location targetLoc = new Location(player.getWorld(), x + 0.5, player.getLocation().getY(), z + 0.5);
        com.smp.smptools.teleport.SafeLocationFinder.findSafeLocation(targetLoc).ifPresent(player::teleport);
    }

    private void sendFakeAdvancement(Player player) {
        // This is a client-side packet, not directly available in Bukkit API.
        // For a simple troll, we can send a message that looks like an advancement.
        player.sendMessage(plugin.getMessageManager().getMessage("troll.fake-advancement", player,
                Map.of("name", player.getName())));
    }

    private void soundSpam(Player player) {
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (!player.isOnline() || count >= 5) {
                    this.cancel();
                    return;
                }
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 1.0f, 1.0f);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 10L); // Every 0.5 seconds
    }

    private void fakeLagMessage(Player player) {
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (!player.isOnline() || count >= 3) {
                    this.cancel();
                    return;
                }
                player.sendMessage(plugin.getMessageManager().getMessage("troll.lag-detected", player));
                count++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every 1 second
    }

    private void fakeJoinLeave(Player player) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.isEmpty())
            return;

        Player randomPlayer = onlinePlayers.get(random.nextInt(onlinePlayers.size()));
        String fakePlayerName = randomPlayer.getName();

        // Fake join message
        Bukkit.broadcast(plugin.getMessageManager().getMessage("troll.fake-join", null,
                Map.of("name", fakePlayerName)));
        new BukkitRunnable() {
            @Override
            public void run() {
                // Fake leave message after a short delay
                Bukkit.broadcast(plugin.getMessageManager().getMessage("troll.fake-leave", null,
                        Map.of("name", fakePlayerName)));
            }
        }.runTaskLater(plugin, 20 * 5); // 5 seconds later
    }

    private void randomItemRename(Player player) {
        Inventory inv = player.getInventory();
        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) != null && inv.getItem(i).getType() != Material.AIR) {
                availableSlots.add(i);
            }
        }

        if (availableSlots.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("troll.no-items-to-rename", player));
            return;
        }

        int randomSlot = availableSlots.get(random.nextInt(availableSlots.size()));
        ItemStack item = inv.getItem(randomSlot);
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            List<String> funnyNames = plugin.getMessageManager().getStringList("troll.funny-names");
            if (funnyNames.isEmpty()) {
                funnyNames = Arrays.asList("<red>Useless Rock</red>", "<green>Magic Stick</green>");
            }
            meta.displayName(MiniMessage.miniMessage().deserialize(funnyNames.get(random.nextInt(funnyNames.size()))));
            item.setItemMeta(meta);
        }
    }

    private void temporaryBlockReplace(Player player) {
        Location loc = player.getLocation();
        int radius = 2;
        Material originalBlockType = Material.AIR; // To store the original block type

        // Find a block to replace near the player
        Block blockToReplace = null;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) { // Check blocks around player's feet and head
                for (int z = -radius; z <= radius; z++) {
                    Block block = loc.clone().add(x, y, z).getBlock();
                    if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                        blockToReplace = block;
                        originalBlockType = block.getType();
                        break;
                    }
                }
                if (blockToReplace != null)
                    break;
            }
            if (blockToReplace != null)
                break;
        }

        if (blockToReplace != null) {
            Block finalBlockToReplace = blockToReplace;
            Material finalOriginalBlockType = originalBlockType;
            finalBlockToReplace.setType(Material.SPONGE); // Replace with sponge
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (finalBlockToReplace.getType() == Material.SPONGE) {
                        finalBlockToReplace.setType(finalOriginalBlockType); // Revert
                    }
                }
            }.runTaskLater(plugin, 20 * 5); // Revert after 5 seconds
        }
    }

    private void soundLoop(Player player) {
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (!player.isOnline() || count >= 10) {
                    this.cancel();
                    return;
                }
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f); // Annoying high pitch
                count++;
            }
        }.runTaskTimer(plugin, 0L, 10L); // Every 0.5 seconds
    }
}
