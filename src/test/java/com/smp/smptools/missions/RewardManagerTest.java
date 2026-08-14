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

        // Fallback / clamped amount
        RewardManager.ParsedItemReward defaultAmount = RewardManager.parseItemReward("item:EMERALD");
        assertNotNull(defaultAmount);
        assertEquals(org.bukkit.Material.EMERALD, defaultAmount.material);
        assertEquals(1, defaultAmount.amount);

        RewardManager.ParsedItemReward invalidNumber = RewardManager.parseItemReward("item:GOLD_INGOT abc");
        assertNotNull(invalidNumber);
        assertEquals(org.bukkit.Material.GOLD_INGOT, invalidNumber.material);
        assertEquals(1, invalidNumber.amount);

        RewardManager.ParsedItemReward negativeClamped = RewardManager.parseItemReward("item:IRON_INGOT -5");
        assertNotNull(negativeClamped);
        assertEquals(org.bukkit.Material.IRON_INGOT, negativeClamped.material);
        assertEquals(1, negativeClamped.amount);
    }

    @Test
    public void testGiveRewardReturnsFalseOnNullPlayer() {
        assertFalse(RewardManager.giveReward(null, "item:DIAMOND 5"));
        assertFalse(RewardManager.giveReward(null, "item:INVALID_XYZ 5"));
        assertFalse(RewardManager.giveReward(null, "item:"));
        assertFalse(RewardManager.giveReward(null, ""));
        assertFalse(RewardManager.giveReward(null, null));
    }
}
