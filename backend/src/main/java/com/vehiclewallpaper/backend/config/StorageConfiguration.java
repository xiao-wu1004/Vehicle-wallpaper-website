package com.vehiclewallpaper.backend.config;

import com.vehiclewallpaper.backend.storage.FilesystemWallpaperStorageDriver;
import com.vehiclewallpaper.backend.storage.S3WallpaperStorageDriver;
import com.vehiclewallpaper.backend.storage.WallpaperStorageDriver;
import com.vehiclewallpaper.backend.storage.WallpaperStorageManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class StorageConfiguration {

    @Bean
    public WallpaperStorageManager wallpaperStorageManager(CatalogProperties catalogProperties,
                                                           StorageProperties storageProperties) {
        List<WallpaperStorageDriver> drivers = new ArrayList<WallpaperStorageDriver>();
        drivers.add(new FilesystemWallpaperStorageDriver(catalogProperties));

        if ("s3".equalsIgnoreCase(storageProperties.getMode())) {
            validateS3Configuration(storageProperties);
            drivers.add(new S3WallpaperStorageDriver(storageProperties));
        }

        return new WallpaperStorageManager(storageProperties.getMode(), drivers);
    }

    private void validateS3Configuration(StorageProperties storageProperties) {
        if (!StringUtils.hasText(storageProperties.getS3Bucket())) {
            throw new IllegalStateException("APP_STORAGE_S3_BUCKET is required when APP_STORAGE_MODE=s3.");
        }
        if (!StringUtils.hasText(storageProperties.getS3Region())) {
            throw new IllegalStateException("APP_STORAGE_S3_REGION is required when APP_STORAGE_MODE=s3.");
        }
    }
}
