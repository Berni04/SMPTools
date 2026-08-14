package com.smp.smptools.missions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RewardManagerTest {

    @Test
    public void testGiveRewardHandlesMalformedStringsWithoutThrowing() {
        assertDoesNotThrow(() -> RewardManager.giveReward(null, "item:DIAMOND abc"));
        assertDoesNotThrow(() -> RewardManager.giveReward(null, "item:INVALID_XYZ 5"));
        assertDoesNotThrow(() -> RewardManager.giveReward(null, "item:"));
        assertDoesNotThrow(() -> RewardManager.giveReward(null, "item:DIAMOND -5"));
        assertDoesNotThrow(() -> RewardManager.giveReward(null, "custom_item:"));
        assertDoesNotThrow(() -> RewardManager.giveReward(null, "command:say hello"));
        assertDoesNotThrow(() -> RewardManager.giveReward(null, ""));
        assertDoesNotThrow(() -> RewardManager.giveReward(null, null));
    }
}
