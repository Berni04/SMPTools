package com.smp.smptools.storage;

public enum StorageType {
    FLATFILE,
    SQLITE,
    MYSQL,
    MARIADB,
    MONGODB;

    public static StorageType parse(String name) {
        if (name == null) return FLATFILE;
        try {
            return StorageType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FLATFILE;
        }
    }
}
