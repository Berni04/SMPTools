package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.listeners.MissionGUIListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import com.smp.smptools.utils.HeadUtils;
import org.bukkit.Material;
import org.bukkit.Color;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class MissionCommand implements CommandExecutor {

    private final SMPTools plugin;
    public static final NamespacedKey MISSION_NPC_KEY = new NamespacedKey(SMPTools.getInstance(), "mission_npc");
    public static final NamespacedKey SANTA_NPC_KEY = new NamespacedKey(SMPTools.getInstance(), "santa_npc");
    public static final Component NPC_NAME = Component.text("Quest Master", NamedTextColor.GOLD, TextDecoration.BOLD);
    public static final Component SANTA_NAME = Component.text("Santa Claus", NamedTextColor.RED, TextDecoration.BOLD);

    public MissionCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            // Admin commands
            if (args[0].equalsIgnoreCase("npc")) {
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("spawn")) {
                        if (args.length > 2 && args[2].equalsIgnoreCase("santa")) {
                            handleSantaSpawn(player);
                        } else {
                            handleNpcSpawn(player);
                        }
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("remove")) {
                        if (args.length > 2) {
                            try {
                                int radius = Integer.parseInt(args[2]);
                                handleNpcRemove(player, radius);
                            } catch (NumberFormatException e) {
                                player.sendMessage(
                                        Component.text("Invalid radius. Please specify a number.", NamedTextColor.RED));
                            }
                        } else {
                            player.sendMessage(
                                    Component.text("Usage: /missions npc remove <radius>", NamedTextColor.RED));
                        }
                        return true;
                    }
                }
                player.sendMessage(Component.text("Usage: /missions npc <spawn [santa]|remove>", NamedTextColor.RED));
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                handleListCommand(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("complete")) {
                handleCompleteCommand(player, args);
                return true;
            }

            if (args[0].equalsIgnoreCase("reset")) {
                handleResetCommand(player, args);
                return true;
            }
        }

        // Default action: open the mission GUI
        MissionGUIListener.openMissionGUI(player, false, "NORMAL");
        return true;
    }

    private void handleNpcSpawn(Player player) {
        if (!player.hasPermission("smptools.missions.admin")) {
            player.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }

        Villager npc = player.getWorld().spawn(player.getLocation(), Villager.class, v -> {
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setCustomNameVisible(true);
            v.customName(NPC_NAME);
            v.setVillagerType(Villager.Type.PLAINS); // Or any other type
            v.setProfession(Villager.Profession.LIBRARIAN); // Looks wise
            v.getPersistentDataContainer().set(MISSION_NPC_KEY, PersistentDataType.BYTE, (byte) 1);
        });

        player.sendMessage(Component.text("Quest Master NPC spawned successfully!", NamedTextColor.GREEN));
    }

    private void handleSantaSpawn(Player player) {
        if (!player.hasPermission("smptools.missions.admin")) {
            player.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }

        Zombie npc = player.getWorld().spawn(player.getLocation(), Zombie.class, z -> {
            z.setAI(false);
            z.setInvulnerable(true);
            z.setSilent(true);
            z.setBaby(false);
            z.setShouldBurnInDay(false);
            z.setCustomNameVisible(true);
            z.customName(SANTA_NAME);

            // Equipment
            z.getEquipment().setHelmet(HeadUtils.getCustomHead(
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTAzNTFkOTJiMWU0ZjVkODM2MDUzODU5MGU2MDk4OTViZDI4M2IwNDc2Zjc0NDIzNjE2NTk3N2Y1YWIyOTllYiJ9fX0="));
            z.getEquipment().setChestplate(createColoredArmor(Material.LEATHER_CHESTPLATE, Color.RED));
            z.getEquipment().setLeggings(createColoredArmor(Material.LEATHER_LEGGINGS, Color.RED));
            z.getEquipment().setBoots(createColoredArmor(Material.LEATHER_BOOTS, Color.BLACK));

            z.getPersistentDataContainer().set(SANTA_NPC_KEY, PersistentDataType.BYTE, (byte) 1);
        });

        player.sendMessage(Component.text("Santa NPC spawned successfully!", NamedTextColor.GREEN));
    }

    private ItemStack createColoredArmor(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        item.setItemMeta(meta);
        return item;
    }

    private void handleNpcRemove(Player player, int radius) {
        if (!player.hasPermission("smptools.missions.admin")) {
            player.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }

        int count = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity.getPersistentDataContainer().has(MISSION_NPC_KEY, PersistentDataType.BYTE) ||
                    entity.getPersistentDataContainer().has(SANTA_NPC_KEY, PersistentDataType.BYTE)) {
                entity.remove();
                count++;
            }
        }

        if (count > 0) {
            player.sendMessage(Component.text("Removed " + count + " NPC(s) within a " + radius + "-block radius.",
                    NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("No Mission NPCs found within that radius.", NamedTextColor.YELLOW));
        }
    }

    private void handleListCommand(Player admin) {
        if (!admin.hasPermission("smptools.missions.admin")) {
            admin.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }

        admin.sendMessage(Component.text("Available Mission IDs:", NamedTextColor.GOLD));
        for (String missionId : plugin.getMissionManager().getAllMissions().keySet()) {
            admin.sendMessage(Component.text("- " + missionId, NamedTextColor.YELLOW)
                    .hoverEvent(Component.text("Click to copy"))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(missionId)));
        }
    }

    private void handleCompleteCommand(Player admin, String[] args) {
        if (!admin.hasPermission("smptools.missions.admin")) {
            admin.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            admin.sendMessage(Component.text("Usage: /missions complete <player> <missionId>", NamedTextColor.RED));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            admin.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        String missionId = args[2];
        if (plugin.getMissionManager().getMission(missionId) == null) {
            admin.sendMessage(Component.text("Mission ID not found.", NamedTextColor.RED));
            return;
        }

        plugin.getMissionManager().forceCompleteMission(target, missionId);
        admin.sendMessage(Component.text("Forcibly completed mission '" + missionId + "' for " + target.getName(),
                NamedTextColor.GREEN));
    }

    private void handleResetCommand(Player admin, String[] args) {
        if (!admin.hasPermission("smptools.missions.admin")) {
            admin.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            admin.sendMessage(Component.text("Usage: /missions reset <player> <missionId>", NamedTextColor.RED));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            admin.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        String missionId = args[2];
        if (plugin.getMissionManager().getMission(missionId) == null) {
            admin.sendMessage(Component.text("Mission ID not found.", NamedTextColor.RED));
            return;
        }

        plugin.getMissionManager().resetMission(target, missionId);
        admin.sendMessage(
                Component.text("Reset mission '" + missionId + "' for " + target.getName(), NamedTextColor.GREEN));
    }
}
