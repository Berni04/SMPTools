package com.smp.smptools.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Custom InventoryHolder to uniquely identify SMPTools GUI inventories.
 * Eliminates title-based spoofing across menus.
 */
public final class GuiHolder implements InventoryHolder {

    public enum MenuType {
        TRADE(false),
        BOUNTY_LIST(true),
        BOUNTY_DETAILS(true),
        BOUNTY_CLAIM(true),
        BOUNTY_DEPOSIT(false),
        ADVENT(true),
        TROLL(true),
        ARTIFACT_POUCH(false),
        HALLOWEEN(true),
        EASTER(true),
        SEASONAL_HUB(true),
        EVENT_DASH(true),
        STATS(true),
        LEADERBOARD(true),
        MISSION(true);

        private final boolean topOnlyClicks;

        MenuType(boolean topOnlyClicks) {
            this.topOnlyClicks = topOnlyClicks;
        }

        /**
         * @return true if interactions with the bottom player inventory should be blocked when this GUI is open.
         */
        public boolean isTopOnly() {
            return topOnlyClicks;
        }
    }

    private final MenuType type;
    private final UUID viewer;
    private Inventory inventory;

    public GuiHolder(MenuType type, UUID viewer) {
        this.type = type;
        this.viewer = viewer;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public MenuType getType() {
        return type;
    }

    public UUID getViewer() {
        return viewer;
    }
}
