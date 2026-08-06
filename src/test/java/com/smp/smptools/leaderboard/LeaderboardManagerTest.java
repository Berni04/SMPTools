package com.smp.smptools.leaderboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LeaderboardManagerTest {

    @Test
    public void testLeaderboardNullPlugin() {
        LeaderboardManager manager = new LeaderboardManager(null);
        assertNotNull(manager);
        var result = manager.getLeaderboard("blocks_broken");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
