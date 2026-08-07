package com.smp.smptools.storage;

import java.util.Locale;

public enum StorageType {
    FLATFILE,
    SQLITE,
    MYSQL,
    MARIADB,
    MONGODB;

    public static StorageType parse(String name) {
        if (name == null) return FLATFILE;
        try {
            return StorageType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return FLATFILE;
        }
    }
}
