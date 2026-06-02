package com.vehiclewallpaper.backend.admin;

import java.time.LocalDateTime;

public class AdminBrandResponse {

    private final Long id;
    private final String slug;
    private final String displayName;
    private final String folderName;
    private final int sortOrder;
    private final int wallpaperCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AdminBrandResponse(Long id,
                              String slug,
                              String displayName,
                              String folderName,
                              int sortOrder,
                              int wallpaperCount,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
        this.id = id;
        this.slug = slug;
        this.displayName = displayName;
        this.folderName = folderName;
        this.sortOrder = sortOrder;
        this.wallpaperCount = wallpaperCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFolderName() {
        return folderName;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public int getWallpaperCount() {
        return wallpaperCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
