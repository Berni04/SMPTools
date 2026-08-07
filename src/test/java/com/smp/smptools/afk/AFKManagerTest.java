package com.smp.smptools.afk;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class AFKManagerTest {

    @Test
    public void testAFKSetOperations() {
        AFKManager manager = new AFKManager(null);
        UUID playerUUID = UUID.randomUUID();

        assertFalse(manager.isAFK(playerUUID));
        manager.getAfkPlayers().add(playerUUID);
        assertTrue(manager.isAFK(playerUUID));

        manager.getAfkPlayers().remove(playerUUID);
        assertFalse(manager.isAFK(playerUUID));
    }
}
