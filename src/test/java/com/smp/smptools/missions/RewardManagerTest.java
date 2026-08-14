package com.smp.smptools.missions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RewardManagerTest {

    @Test
    public void testParseItemReward() {
        assertNull(RewardManager.parseItemReward(null));
        assertNull(RewardManager.parseItemReward(""));
        assertNull(RewardManager.parseItemReward("item:"));
        assertNull(RewardManager.parseItemReward("item:   "));
        assertNull(RewardManager.parseItemReward("item:INVALID_MATERIAL_XYZ 5"));
        assertNull(RewardManager.parseItemReward("not_an_item:DIAMOND 5"));

        RewardManager.ParsedItemReward valid = RewardManager.parseItemReward("item:DIAMOND 5");
        assertNotNull(valid);
        assertEquals(org.bukkit.Material.DIAMOND, valid.material);
        assertEquals(5, valid.amount);

        RewardManager.ParsedItemReward defaultAmount = RewardManager.parseItemReward("item:EMERALD");
        assertNotNull(defaultAmount);
        assertEquals(org.bukkit.Material.EMERALD, defaultAmount.material);
        assertEquals(1, defaultAmount.amount);

        assertNull(RewardManager.parseItemReward("item:GOLD_INGOT abc"));
        assertNull(RewardManager.parseItemReward("item:IRON_INGOT -5"));
        assertNull(RewardManager.parseItemReward("item:DIAMOND 0"));
    }

    @Test
    public void testGiveRewardReturnsFalseOnNullPlayer() {
        assertFalse(RewardManager.giveReward(null, "item:DIAMOND 5"));
        assertFalse(RewardManager.giveReward(null, "item:INVALID_XYZ 5"));
        assertFalse(RewardManager.giveReward(null, "item:"));
        assertFalse(RewardManager.giveReward(null, ""));
        assertFalse(RewardManager.giveReward(null, null));
    }

    @Test
    public void testIsValidReward() {
        assertTrue(RewardManager.isValidReward("item:DIAMOND 5"));
        assertTrue(RewardManager.isValidReward("item:EMERALD"));
        assertTrue(RewardManager.isValidReward("custom_item:chromatic_elytra"));
        assertTrue(RewardManager.isValidReward("command:eco give %player% 100"));

        assertFalse(RewardManager.isValidReward(null));
        assertFalse(RewardManager.isValidReward(""));
        assertFalse(RewardManager.isValidReward("item:INVALID_XYZ 5"));
        assertFalse(RewardManager.isValidReward("custom_item:unknown"));
        assertFalse(RewardManager.isValidReward("command:op %player%"));
    }
}
