package com.vehiclewallpaper.backend.storage;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WallpaperStorageManager {

    private final String defaultProvider;
    private final Map<String, WallpaperStorageDriver> driversByProvider;

    public WallpaperStorageManager(String defaultProvider, List<WallpaperStorageDriver> drivers) {
        this.defaultProvider = normalizeProvider(defaultProvider);
        this.driversByProvider = new LinkedHashMap<String, WallpaperStorageDriver>();
        for (WallpaperStorageDriver driver : drivers) {
            this.driversByProvider.put(normalizeProvider(driver.getProviderId()), driver);
        }
        if (!this.driversByProvider.containsKey(this.defaultProvider)) {
            throw new IllegalStateException("No wallpaper storage driver is configured for provider: " + defaultProvider);
        }
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public StoredAsset store(String storageKey, MultipartFile file) throws IOException {
        return requireDriver(defaultProvider).store(storageKey, file);
    }

    public StoredAsset move(String provider, String sourceKey, String targetKey) throws IOException {
        return requireDriver(provider).move(sourceKey, targetKey);
    }

    public void delete(String provider, String storageKey) throws IOException {
        requireDriver(provider).delete(storageKey);
    }

    public String toPublicUrl(String provider, String storageKey) {
        return requireDriver(provider).toPublicUrl(storageKey);
    }

    public boolean isFilesystem(String provider) {
        return FilesystemWallpaperStorageDriver.PROVIDER_ID.equals(normalizeProvider(provider));
    }

    private WallpaperStorageDriver requireDriver(String provider) {
        WallpaperStorageDriver driver = driversByProvider.get(normalizeProvider(provider));
        if (driver == null) {
            throw new IllegalStateException("Unsupported wallpaper storage provider: " + provider);
        }
        return driver;
    }

    private String normalizeProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            return "";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
