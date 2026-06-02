package com.vehiclewallpaper.backend.storage;

public class StoredAsset {

    private final String provider;
    private final String storageKey;
    private final String publicUrl;

    public StoredAsset(String provider, String storageKey, String publicUrl) {
        this.provider = provider;
        this.storageKey = storageKey;
        this.publicUrl = publicUrl;
    }

    public String getProvider() {
        return provider;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getPublicUrl() {
        return publicUrl;
    }
}
