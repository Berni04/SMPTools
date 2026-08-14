package com.smp.smptools.events;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EventManagerTest {

    @Test
    public void testParseRewardStringValid() {
        EventManager.ParsedEventReward cmd = EventManager.parseRewardString("cmd:eco give %player% 500");
        assertNotNull(cmd);
        assertEquals(EventManager.RewardType.COMMAND, cmd.getType());
        assertEquals("eco give %player% 500", cmd.getCommand());
        assertEquals(0, cmd.getRetryCount());

        EventManager.ParsedEventReward item = EventManager.parseRewardString("item:DIAMOND 3");
        assertNotNull(item);
        assertEquals(EventManager.RewardType.ITEM, item.getType());
        assertEquals(Material.DIAMOND, item.getMaterial());
        assertEquals(3, item.getAmount());
        assertEquals(0, item.getRetryCount());

        EventManager.ParsedEventReward itemDefault = EventManager.parseRewardString("item:GOLD_INGOT");
        assertNotNull(itemDefault);
        assertEquals(EventManager.RewardType.ITEM, itemDefault.getType());
        assertEquals(Material.GOLD_INGOT, itemDefault.getMaterial());
        assertEquals(1, itemDefault.getAmount());
    }

    @Test
    public void testParseRewardStringWithRetryMetadata() {
        EventManager.ParsedEventReward cmdWithRetry = EventManager.parseRewardString("cmd:eco give %player% 500#retry:2");
        assertNotNull(cmdWithRetry);
        assertEquals(EventManager.RewardType.COMMAND, cmdWithRetry.getType());
        assertEquals("eco give %player% 500", cmdWithRetry.getCommand());
        assertEquals(2, cmdWithRetry.getRetryCount());

        EventManager.ParsedEventReward itemWithRetry = EventManager.parseRewardString("item:EMERALD 5#retry:1");
        assertNotNull(itemWithRetry);
        assertEquals(EventManager.RewardType.ITEM, itemWithRetry.getType());
        assertEquals(Material.EMERALD, itemWithRetry.getMaterial());
        assertEquals(5, itemWithRetry.getAmount());
        assertEquals(1, itemWithRetry.getRetryCount());
    }

    @Test
    public void testParseRewardStringRejectsMalformed() {
        assertNull(EventManager.parseRewardString(null));
        assertNull(EventManager.parseRewardString(""));
        assertNull(EventManager.parseRewardString("   "));
        assertNull(EventManager.parseRewardString("cmd:"));
        assertNull(EventManager.parseRewardString("cmd:   "));
        assertNull(EventManager.parseRewardString("item:"));
        assertNull(EventManager.parseRewardString("item:   "));
        assertNull(EventManager.parseRewardString("item:INVALID_XYZ 5"));
        assertNull(EventManager.parseRewardString("item:DIAMOND notanumber"));
        assertNull(EventManager.parseRewardString("item:DIAMOND -5"));
        assertNull(EventManager.parseRewardString("item:DIAMOND 0"));
        assertNull(EventManager.parseRewardString("unknown:DIAMOND 5"));
        assertNull(EventManager.parseRewardString("cmd:eco give %player% 100#retry:notanumber"));
        assertNull(EventManager.parseRewardString("cmd:eco give %player% 100#retry:-1"));
    }
}
