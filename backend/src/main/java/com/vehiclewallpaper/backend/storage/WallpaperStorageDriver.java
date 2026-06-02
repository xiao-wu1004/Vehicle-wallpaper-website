package com.vehiclewallpaper.backend.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface WallpaperStorageDriver {

    String getProviderId();

    StoredAsset store(String storageKey, MultipartFile file) throws IOException;

    StoredAsset move(String sourceKey, String targetKey) throws IOException;

    void delete(String storageKey) throws IOException;

    String toPublicUrl(String storageKey);
}
