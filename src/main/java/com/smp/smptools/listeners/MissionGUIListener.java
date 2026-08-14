package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.missions.Mission;
import com.smp.smptools.missions.MissionManager;
import com.smp.smptools.missions.MissionType;
import com.smp.smptools.missions.RewardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MissionGUIListener implements Listener {

    private static final String MAIN_GUI_TITLE = "Mission Control";
    private static final String MAIN_GUI_TITLE_NPC = "Mission Control (NPC)"; // Title for NPC interaction
    private static final String AVAILABLE_MISSIONS_TITLE = "Available Missions";
    private static final String IN_PROGRESS_MISSIONS_TITLE = "In Progress Missions";
    private static final String COMPLETED_MISSIONS_TITLE = "Completed Missions";
    private static final String CONFIRM_ACCEPT_TITLE = "Confirm Mission Acceptance";
    private static final String COLOR_GUI_TITLE = "Choose a Trail Color";
    private static final String QUESTLINE_SELECTION_TITLE = "Choose Your Path";

    private static MissionManager missionManager;

    public MissionGUIListener(SMPTools plugin) {
        missionManager = plugin.getMissionManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        if (event.getCurrentItem() == null)
            return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check for both standard and NPC titles
        if (title.equals(MAIN_GUI_TITLE) || title.equals(MAIN_GUI_TITLE_NPC) ||
                title.equals(AVAILABLE_MISSIONS_TITLE) || title.equals(IN_PROGRESS_MISSIONS_TITLE) ||
                title.equals(COMPLETED_MISSIONS_TITLE) || title.equals(CONFIRM_ACCEPT_TITLE) ||
                title.equals(COLOR_GUI_TITLE) || title.equals(QUESTLINE_SELECTION_TITLE)) {

            event.setCancelled(true);

            // Determine context based on title (simple heuristic for now, could pass via
            // holder)
            boolean isNpcContext = title.equals(MAIN_GUI_TITLE_NPC) ||
                    (event.getInventory().getHolder() instanceof MissionGUIHolder
                            && ((MissionGUIHolder) event.getInventory().getHolder()).isNpc());

            switch (title) {
                case MAIN_GUI_TITLE:
                case MAIN_GUI_TITLE_NPC:
                    handleMainMenuClick(event, player, isNpcContext);
                    break;
                case AVAILABLE_MISSIONS_TITLE:
                    handleAvailableMissionsClick(event, player, isNpcContext);
                    break;
                case IN_PROGRESS_MISSIONS_TITLE:
                    handleInProgressMissionsClick(event, player, isNpcContext);
                    break;
                case COMPLETED_MISSIONS_TITLE:
                    handleCompletedMissionsClick(event, player, isNpcContext);
                    break;
                case CONFIRM_ACCEPT_TITLE:
                    handleConfirmationClick(event, player, isNpcContext);
                    break;
                case COLOR_GUI_TITLE:
                    handleColorGUIClick(event, player);
                    break;
                case QUESTLINE_SELECTION_TITLE:
                    handleQuestlineSelectionClick(event, player);
                    break;
            }
        }
    }

    private void handleMainMenuClick(InventoryClickEvent event, Player player, boolean isNpc) {
        switch (event.getCurrentItem().getType()) {
            case BOOK:
                openAvailableMissionsGUI(player, isNpc);
                break;
            case COMPASS:
                openInProgressMissionsGUI(player, isNpc);
                break;
            case BEACON:
                openCompletedMissionsGUI(player, isNpc);
                break;
        }
    }

    private void handleAvailableMissionsClick(InventoryClickEvent event, Player player, boolean isNpc) {
        if (isBackButton(event.getCurrentItem())) {
            String category = "NORMAL";
            if (event.getView().getTopInventory().getHolder() instanceof MissionGUIHolder) {
                category = ((MissionGUIHolder) event.getView().getTopInventory().getHolder()).getCategory();
            }
            openMissionGUI(player, isNpc, category);
            return;
        }

        Mission clickedMission = getMissionFromItem(event.getCurrentItem());
        if (clickedMission != null) {
            openConfirmationGUI(player, clickedMission, isNpc);
        }
    }

    private void handleInProgressMissionsClick(InventoryClickEvent event, Player player, boolean isNpc) {
        if (isBackButton(event.getCurrentItem())) {
            String category = "NORMAL";
            if (event.getView().getTopInventory().getHolder() instanceof MissionGUIHolder) {
                category = ((MissionGUIHolder) event.getView().getTopInventory().getHolder()).getCategory();
            }
            openMissionGUI(player, isNpc, category);
            return;
        }

        Mission clickedMission = getMissionFromItem(event.getCurrentItem());
        if (clickedMission == null)
            return;

        if (clickedMission.getType() == MissionType.SUBMIT_ITEM) {
            Material requiredMaterial = Material.valueOf(clickedMission.getObjective());
            int requiredAmount = clickedMission.getAmount();
            int currentAmount = countItems(player, requiredMaterial);

            if (currentAmount >= requiredAmount) {
                removeItems(player, requiredMaterial, requiredAmount);
                missionManager.forceCompleteMission(player, clickedMission.getId());
                player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.mission-completed-items"));
                openCompletedMissionsGUI(player, isNpc);
            } else {
                player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.not-enough-items", player, Map.of("amount", String.valueOf(requiredAmount), "material", requiredMaterial.name())));
                player.closeInventory();
            }
        }
    }

    private void handleCompletedMissionsClick(InventoryClickEvent event, Player player, boolean isNpc) {
        if (isBackButton(event.getCurrentItem())) {
            String category = "NORMAL";
            if (event.getView().getTopInventory().getHolder() instanceof MissionGUIHolder) {
                category = ((MissionGUIHolder) event.getView().getTopInventory().getHolder()).getCategory();
            }
            openMissionGUI(player, isNpc, category);
            return;
        }

        Mission clickedMission = getMissionFromItem(event.getCurrentItem());
        if (clickedMission == null)
            return;

        MissionManager.PlayerMissionData playerData = missionManager.getPlayerData(player);

        if (playerData.getCompletedMissions().contains(clickedMission.getId()) &&
                !playerData.getClaimedMissions().contains(clickedMission.getId())) {

            if (!isNpc) {
                player.sendMessage(
                        SMPTools.getInstance().getMessageManager().getMessage("missions.return-to-npc", player, Map.of("npc", "Quest Master")));
                player.closeInventory();
                return;
            }

            // Check for Chromatic Elytra first to open color selection
            boolean hasChromaticElytra = false;
            for (String reward : clickedMission.getRewards()) {
                if (reward.contains("custom_item:chromatic_elytra")) {
                    hasChromaticElytra = true;
                    break;
                }
            }

            if (hasChromaticElytra) {
                openColorSelectionGUI(player, clickedMission.getId());
            } else {
                playerData.getClaimedMissions().add(clickedMission.getId());
                if (!missionManager.saveSinglePlayerData(player.getUniqueId())) {
                    playerData.getClaimedMissions().remove(clickedMission.getId());
                    player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Failed to save mission claim. Please try again.</red>"));
                    return;
                }
                for (String reward : clickedMission.getRewards()) {
                    try {
                        RewardManager.giveReward(player, reward);
                    } catch (Exception e) {
                        SMPTools.getInstance().getLogger().warning("Exception giving mission reward '" + reward + "' to " + player.getName() + ": " + e.getMessage());
                    }
                }
                player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.reward-claimed", player));
                // Re-open the GUI to update the item state
                openCompletedMissionsGUI(player, isNpc);
            }
        } else if (playerData.getClaimedMissions().contains(clickedMission.getId())) {
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.reward-already-claimed"));
        }
    }

    private void handleConfirmationClick(InventoryClickEvent event, Player player, boolean isNpc) {
        Mission clickedMission = getMissionFromItem(event.getCurrentItem());
        if (clickedMission == null) {
            player.closeInventory();
            return;
        }

        if (event.getCurrentItem().getType() == Material.GREEN_WOOL) {
            if (!missionManager.getPlayerData(player).getActiveMissions().isEmpty()) {
                player.sendMessage(
                        SMPTools.getInstance().getMessageManager().getMessage("missions.only-one-active"));
                player.closeInventory();
                return;
            }

            missionManager.getPlayerData(player).getActiveMissions().add(clickedMission.getId());
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.mission-accepted", player, Map.of("mission_name", clickedMission.getName())));
            openInProgressMissionsGUI(player, isNpc);
        } else if (event.getCurrentItem().getType() == Material.RED_WOOL) {
            openAvailableMissionsGUI(player, isNpc);
        }
    }

    private void handleColorGUIClick(InventoryClickEvent event, Player player) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null)
            return;

        String color = "WHITE";
        switch (clickedItem.getType()) {
            case RED_WOOL:
                color = "RED";
                break;
            case BLUE_WOOL:
                color = "BLUE";
                break;
            case LIME_WOOL:
                color = "GREEN";
                break;
            case PURPLE_WOOL:
                color = "PURPLE";
                break;
            case ORANGE_WOOL:
                color = "ORANGE";
                break;
            case YELLOW_WOOL:
                color = "YELLOW";
                break;
            case BLACK_WOOL:
                color = "BLACK";
                break;
            case WHITE_WOOL:
                color = "WHITE";
                break;
            case CYAN_WOOL:
                color = "RAINBOW";
                break;
        }

        String missionId = getMissionIdFromLore(clickedItem);
        if (missionId == null)
            return;

        MissionManager.PlayerMissionData playerData = missionManager.getPlayerData(player);
        if (playerData.getClaimedMissions().contains(missionId)) {
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.reward-already-claimed"));
            player.closeInventory();
            return;
        }

        boolean given = RewardManager.giveChromaticElytra(player, color);
        if (given) {
            playerData.getClaimedMissions().add(missionId);
            if (!missionManager.saveSinglePlayerData(player.getUniqueId())) {
                playerData.getClaimedMissions().remove(missionId);
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Failed to save mission claim. Please try again.</red>"));
                return;
            }
            player.closeInventory();
        } else {
            player.closeInventory();
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Failed to create Chromatic Elytra reward. Please contact an admin.</red>"));
        }
    }

    private void handleQuestlineSelectionClick(InventoryClickEvent event, Player player) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null)
            return;

        String questline = null;
        switch (clickedItem.getType()) {
            case CRAFTING_TABLE:
                questline = "CHRISTMAS_WORKSHOP";
                break;
            case GREEN_WOOL:
                questline = "CHRISTMAS_GRINCH";
                break;
            case CAKE:
                questline = "CHRISTMAS_FEAST";
                break;
        }

        if (questline != null) {
            MissionManager.PlayerMissionData playerData = missionManager.getPlayerData(player);
            playerData.setSelectedQuestline(questline);
            missionManager.savePlayerData(); // Save immediately
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.path-chosen"));
            openMissionGUI(player, true, questline);
        }
    }

    // --- GUI Creation Methods ---

    public static void openMissionGUI(Player player, boolean isNpc, String category) {
        String title = isNpc ? MAIN_GUI_TITLE_NPC : MAIN_GUI_TITLE;
        Inventory gui = Bukkit.createInventory(new MissionGUIHolder(isNpc, category), 27, Component.text(title));
        gui.setItem(11, createGuiItem(Material.BOOK, "&a&lAvailable Missions",
                List.of("§7Click to see missions you can start.")));
        gui.setItem(13,
                createGuiItem(Material.COMPASS, "&e&lIn Progress", List.of("§7Click to see your active missions.")));
        gui.setItem(15, createGuiItem(Material.BEACON, "&b&lCompleted Missions",
                List.of("§7Click to see missions you have finished.")));
        player.openInventory(gui);
    }

    // Overload for backward compatibility if needed, defaults to false and NORMAL
    public static void openMissionGUI(Player player) {
        openMissionGUI(player, false, "NORMAL");
    }

    public static void openMissionGUI(Player player, boolean isNpc) {
        openMissionGUI(player, isNpc, "NORMAL");
    }

    private void openAvailableMissionsGUI(Player player, boolean isNpc) {
        String category = "NORMAL";
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof MissionGUIHolder) {
            category = ((MissionGUIHolder) player.getOpenInventory().getTopInventory().getHolder()).getCategory();
        }

        Inventory gui = Bukkit.createInventory(new MissionGUIHolder(isNpc, category), 54,
                Component.text(AVAILABLE_MISSIONS_TITLE));
        MissionManager.PlayerMissionData playerData = missionManager.getPlayerData(player);
        for (Mission mission : missionManager.getAllMissions().values()) {
            if (!mission.getCategory().equalsIgnoreCase(category))
                continue;

            if (!playerData.getActiveMissions().contains(mission.getId()) &&
                    !playerData.getCompletedMissions().contains(mission.getId())) {

                // Check prerequisites
                boolean prerequisitesMet = true;
                if (mission.getPrerequisites() != null && !mission.getPrerequisites().isEmpty()) {
                    for (String prereqId : mission.getPrerequisites()) {
                        if (!playerData.getCompletedMissions().contains(prereqId)) {
                            prerequisitesMet = false;
                            break;
                        }
                    }
                }

                if (prerequisitesMet) {
                    gui.addItem(createMissionItem(mission, 0, MissionStatus.AVAILABLE, false, player));
                }
            }
        }
        addBackButton(gui, 49);
        player.openInventory(gui);
    }

    private void openInProgressMissionsGUI(Player player, boolean isNpc) {
        String category = "NORMAL";
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof MissionGUIHolder) {
            category = ((MissionGUIHolder) player.getOpenInventory().getTopInventory().getHolder()).getCategory();
        }

        Inventory gui = Bukkit.createInventory(new MissionGUIHolder(isNpc, category), 54,
                Component.text(IN_PROGRESS_MISSIONS_TITLE));
        MissionManager.PlayerMissionData playerData = missionManager.getPlayerData(player);
        for (String missionId : playerData.getActiveMissions()) {
            Mission mission = missionManager.getMission(missionId);
            if (mission == null || playerData.getCompletedMissions().contains(missionId))
                continue;
            if (!mission.getCategory().equalsIgnoreCase(category))
                continue;

            int progress = playerData.getMissionProgress().getOrDefault(missionId, 0);
            if (mission.getType() == MissionType.SUBMIT_ITEM) {
                progress = countItems(player, Material.valueOf(mission.getObjective()));
            }
            gui.addItem(createMissionItem(mission, progress, MissionStatus.IN_PROGRESS, false, player));
        }
        addBackButton(gui, 49);
        player.openInventory(gui);
    }

    private void openCompletedMissionsGUI(Player player, boolean isNpc) {
        String category = "NORMAL";
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof MissionGUIHolder) {
            category = ((MissionGUIHolder) player.getOpenInventory().getTopInventory().getHolder()).getCategory();
        }

        Inventory gui = Bukkit.createInventory(new MissionGUIHolder(isNpc, category), 54,
                Component.text(COMPLETED_MISSIONS_TITLE));
        MissionManager.PlayerMissionData playerData = missionManager.getPlayerData(player);
        for (String missionId : playerData.getCompletedMissions()) {
            Mission mission = missionManager.getMission(missionId);
            if (mission == null)
                continue;
            if (!mission.getCategory().equalsIgnoreCase(category))
                continue;

            boolean isClaimed = playerData.getClaimedMissions().contains(missionId);
            gui.addItem(createMissionItem(mission, mission.getAmount(), MissionStatus.COMPLETED, isClaimed, player));
        }
        addBackButton(gui, 49);
        player.openInventory(gui);
    }

    private void openConfirmationGUI(Player player, Mission mission, boolean isNpc) {
        String category = mission.getCategory();
        Inventory gui = Bukkit.createInventory(new MissionGUIHolder(isNpc, category), 27,
                Component.text(CONFIRM_ACCEPT_TITLE));
        ItemStack confirmItem = createGuiItem(Material.GREEN_WOOL, "&a&lConfirm",
                List.of("§7Accept this mission.", "mission_id:" + mission.getId()));
        ItemStack cancelItem = createGuiItem(Material.RED_WOOL, "&c&lCancel", List.of("§7Do not accept this mission."));
        gui.setItem(11, confirmItem);
        gui.setItem(15, cancelItem);
        player.openInventory(gui);
    }

    private static void openColorSelectionGUI(Player player, String missionId) {
        Inventory gui = Bukkit.createInventory(null, 9, Component.text(COLOR_GUI_TITLE));
        gui.setItem(0, createGuiItem(Material.RED_WOOL, "&cRed Trail", List.of("mission_id:" + missionId)));
        gui.setItem(1, createGuiItem(Material.BLUE_WOOL, "&9Blue Trail", List.of("mission_id:" + missionId)));
        gui.setItem(2, createGuiItem(Material.LIME_WOOL, "&aGreen Trail", List.of("mission_id:" + missionId)));
        gui.setItem(3, createGuiItem(Material.PURPLE_WOOL, "&5Purple Trail", List.of("mission_id:" + missionId)));
        gui.setItem(4, createGuiItem(Material.ORANGE_WOOL, "&6Orange Trail", List.of("mission_id:" + missionId)));
        gui.setItem(5, createGuiItem(Material.YELLOW_WOOL, "&eYellow Trail", List.of("mission_id:" + missionId)));
        gui.setItem(6, createGuiItem(Material.BLACK_WOOL, "&0Black Trail", List.of("mission_id:" + missionId)));
        gui.setItem(7, createGuiItem(Material.WHITE_WOOL, "&fWhite Trail", List.of("mission_id:" + missionId)));
        gui.setItem(8, createGuiItem(Material.CYAN_WOOL, "&bRainbow Trail", List.of("mission_id:" + missionId)));
        player.openInventory(gui);
    }

    public static void openQuestlineSelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, Component.text(QUESTLINE_SELECTION_TITLE));

        gui.setItem(2, createGuiItem(Material.CRAFTING_TABLE, "&a&lSanta's Workshop Recovery",
                List.of("§7Help Santa rebuild after the storm.", "", "§eClick to select this path!")));

        gui.setItem(4, createGuiItem(Material.GREEN_WOOL, "&c&lThe Grinch's Sabotage",
                List.of("§7Recover stolen items and clean up the mess.", "", "§eClick to select this path!")));

        gui.setItem(6, createGuiItem(Material.CAKE, "&6&lThe Great Feast",
                List.of("§7Gather ingredients for a massive dinner.", "", "§eClick to select this path!")));

        player.openInventory(gui);
    }

    // --- Utility Methods ---

    private enum MissionStatus {
        AVAILABLE, IN_PROGRESS, COMPLETED
    }

    private static ItemStack createMissionItem(Mission mission, int currentProgress, MissionStatus status,
            boolean isClaimed, Player player) {
        boolean isClaimable = currentProgress >= mission.getAmount();
        Material material;
        if (status == MissionStatus.COMPLETED) {
            material = isClaimed ? Material.WRITTEN_BOOK : Material.ENCHANTED_BOOK;
        } else {
            material = (status == MissionStatus.AVAILABLE || !isClaimable) ? Material.WRITABLE_BOOK
                    : Material.ENCHANTED_BOOK;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(legacy(mission.getName()));
        List<Component> lore = new ArrayList<>();
                lore.add(legacy(mission.getDescription()));
        lore.add(Component.text(""));
        if (status != MissionStatus.AVAILABLE) {
            double percentage = Math.min(1.0, (double) currentProgress / mission.getAmount());
            lore.add(SMPTools.getInstance().getMessageManager().getMessage("missions.progress", player,
                    Map.of("current", String.valueOf(currentProgress), "total", String.valueOf(mission.getAmount()))));
            lore.add(LEGACY.deserialize(createProgressBar(percentage)));
            lore.add(Component.text(""));
        }
        switch (status) {
            case AVAILABLE:
                lore.add(SMPTools.getInstance().getMessageManager().getMessage("missions.click-accept", player));
                break;
            case COMPLETED:
                if (isClaimed) {
                    lore.add(SMPTools.getInstance().getMessageManager().getMessage("missions.reward-claimed-label", player));
                } else {
                    lore.add(SMPTools.getInstance().getMessageManager().getMessage("missions.click-claim", player));
                    meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                break;
            case IN_PROGRESS:
                lore.add(SMPTools.getInstance().getMessageManager().getMessage("missions.status-in-progress", player));
                break;
        }
        String escapedId = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().escapeTags(mission.getId());
        lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                SMPTools.getInstance().getMessageManager().getRawMessage("missions.mission-id-lore")
                        .replace("{id}", escapedId)));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static Component legacy(String text) {
        return LEGACY.deserialize(text.replace('&', '§'));
    }

    private static ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(legacy(name));
        meta.lore(lore.stream().map(MissionGUIListener::legacy).collect(Collectors.toList()));
        item.setItemMeta(meta);
        return item;
    }

    private static String createProgressBar(double percentage) {
        int barLength = 20;
        int filled = (int) (barLength * percentage);
        return "§a" + "|".repeat(filled) + "§7" + "|".repeat(barLength - filled);
    }

    private void addBackButton(Inventory gui, int slot) {
        gui.setItem(slot, createGuiItem(Material.ARROW, "&c&lBack", List.of("§7Return to the main menu.")));
    }

    private boolean isBackButton(ItemStack item) {
        return item != null && item.getType() == Material.ARROW;
    }

    private Mission getMissionFromItem(ItemStack item) {
        String missionId = getMissionIdFromLore(item);
        return missionId != null ? missionManager.getMission(missionId) : null;
    }

    private String getMissionIdFromLore(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore())
            return null;
        for (Component component : item.getItemMeta().lore()) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
            if (plain.contains("mission_id:")) {
                String idPart = plain.substring(plain.indexOf("mission_id:") + "mission_id:".length()).trim();
                int end = idPart.indexOf(' ');
                if (end != -1) {
                    idPart = idPart.substring(0, end);
                }
                return idPart;
            }
        }
        return null;
    }

    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                if (item.getAmount() > remaining) {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                } else {
                    remaining -= item.getAmount();
                    item.setAmount(0);
                }
                if (remaining <= 0)
                    break;
            }
        }
    }

    // Helper class to track GUI context
    public static class MissionGUIHolder implements org.bukkit.inventory.InventoryHolder {
        private final boolean isNpc;
        private final String category;
        private Inventory inventory;

        public MissionGUIHolder(boolean isNpc, String category) {
            this.isNpc = isNpc;
            this.category = category;
        }

        public MissionGUIHolder(boolean isNpc) {
            this(isNpc, "NORMAL");
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public boolean isNpc() {
            return isNpc;
        }

        public String getCategory() {
            return category;
        }

        @Override
        public Inventory getInventory() {
            if (inventory == null) {
                inventory = Bukkit.createInventory(this, 54);
            }
            return inventory;
        }
    }
}
