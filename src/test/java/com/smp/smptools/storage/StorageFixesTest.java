package com.smp.smptools.storage;

import com.smp.smptools.locks.LockManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class StorageFixesTest {

    @Test
    public void testTurkishLocaleStorageTypeParse() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertEquals(StorageType.SQLITE, StorageType.parse("sqlite"));
            assertEquals(StorageType.MYSQL, StorageType.parse("mysql"));
            assertEquals(StorageType.MARIADB, StorageType.parse("mariadb"));
            assertEquals(StorageType.MONGODB, StorageType.parse("mongodb"));
            assertEquals(StorageType.FLATFILE, StorageType.parse("flatfile"));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testNewContainerTypesSupportedInLockManager() {
        LockManager lockManager = new LockManager(null);

        Block brewingStand = createBlockWithMaterial(Material.BREWING_STAND);
        Block chiseledBookshelf = createBlockWithMaterial(Material.CHISELED_BOOKSHELF);
        Block chest = createBlockWithMaterial(Material.CHEST);
        Block stone = createBlockWithMaterial(Material.STONE);

        assertTrue(lockManager.isContainer(brewingStand), "BREWING_STAND should be recognized as a container");
        assertTrue(lockManager.isContainer(chiseledBookshelf), "CHISELED_BOOKSHELF should be recognized as a container");
        assertTrue(lockManager.isContainer(chest), "CHEST should be recognized as a container");
        assertFalse(lockManager.isContainer(stone), "STONE should not be recognized as a container");
    }

    private Block createBlockWithMaterial(Material material) {
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getType")) {
                        return material;
                    }
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    return null;
                }
        );
    }
}
