package com.smp.smptools.events;

import com.smp.smptools.events.minievents.MiniEventListener;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MiniEventTransformTest {

    @Test
    public void testZombieConversionPreservesSpawnerMobExclusion() {
        NamespacedKey spawnerKey = NamespacedKey.fromString("smptools:spawner_mob");

        Map<NamespacedKey, Object> zombiePdcMap = new HashMap<>();
        zombiePdcMap.put(spawnerKey, (byte) 1);

        PersistentDataContainer zombiePdc = (PersistentDataContainer) Proxy.newProxyInstance(
                PersistentDataContainer.class.getClassLoader(),
                new Class<?>[]{PersistentDataContainer.class},
                (proxy, method, args) -> {
                    if ("has".equals(method.getName())) {
                        return zombiePdcMap.containsKey(args[0]);
                    }
                    if ("get".equals(method.getName())) {
                        return zombiePdcMap.get(args[0]);
                    }
                    return null;
                }
        );

        Zombie zombie = (Zombie) Proxy.newProxyInstance(
                Zombie.class.getClassLoader(),
                new Class<?>[]{Zombie.class},
                (proxy, method, args) -> {
                    if ("getPersistentDataContainer".equals(method.getName())) {
                        return zombiePdc;
                    }
                    if ("hasMetadata".equals(method.getName())) {
                        return false;
                    }
                    return null;
                }
        );

        Map<NamespacedKey, Object> drownedPdcMap = new HashMap<>();
        PersistentDataContainer drownedPdc = (PersistentDataContainer) Proxy.newProxyInstance(
                PersistentDataContainer.class.getClassLoader(),
                new Class<?>[]{PersistentDataContainer.class},
                (proxy, method, args) -> {
                    if ("set".equals(method.getName())) {
                        drownedPdcMap.put((NamespacedKey) args[0], args[2]);
                        return null;
                    }
                    if ("has".equals(method.getName())) {
                        return drownedPdcMap.containsKey(args[0]);
                    }
                    return null;
                }
        );

        Drowned drowned = (Drowned) Proxy.newProxyInstance(
                Drowned.class.getClassLoader(),
                new Class<?>[]{Drowned.class},
                (proxy, method, args) -> {
                    if ("getPersistentDataContainer".equals(method.getName())) {
                        return drownedPdc;
                    }
                    if ("setMetadata".equals(method.getName())) {
                        return null;
                    }
                    return null;
                }
        );

        MiniEventListener listener = new MiniEventListener(null, null);

        EntityTransformEvent transformEvent = new EntityTransformEvent(
                zombie,
                Collections.singletonList(drowned),
                EntityTransformEvent.TransformReason.DROWNED
        );

        listener.onEntityTransform(transformEvent);

        assertTrue(drownedPdcMap.containsKey(spawnerKey), "Transformed entity must inherit spawner_mob PDC key");
        assertEquals((byte) 1, drownedPdcMap.get(spawnerKey), "Spawner mob marker value must be preserved");
    }
}
