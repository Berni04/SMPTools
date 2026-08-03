package com.smp.smptools.storage;

import com.smp.smptools.SMPTools;

public class StorageManager {

    private final SMPTools plugin;
    private final StorageType type;
    private final StorageProvider provider;

    public StorageManager(SMPTools plugin) {
        this.plugin = plugin;
        String typeStr = plugin.getConfig().getString("storage.type", "FLATFILE");
        this.type = StorageType.parse(typeStr);

        switch (type) {
            case SQLITE:
            case MYSQL:
            case MARIADB:
                this.provider = new JdbcStorageProvider(plugin, type);
                break;
            case MONGODB:
                this.provider = new MongoStorageProvider(plugin);
                break;
            case FLATFILE:
            default:
                this.provider = new FlatFileStorageProvider(plugin);
                break;
        }

        this.provider.init();
    }

    public StorageType getType() {
        return type;
    }

    public StorageProvider getProvider() {
        return provider;
    }

    public void shutdown() {
        if (provider != null) {
            provider.shutdown();
        }
    }
}
