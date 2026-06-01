package com.vehiclewallpaper.backend.admin;

import java.time.LocalDateTime;

public class AdminWallpaperResponse {

    private final Long id;
    private final String slug;
    private final String brandSlug;
    private final String brandName;
    private final String title;
    private final String fileName;
    private final String previewUrl;
    private final String fullUrl;
    private final String downloadUrl;
    private final int sortOrder;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AdminWallpaperResponse(Long id,
                                  String slug,
                                  String brandSlug,
                                  String brandName,
                                  String title,
                                  String fileName,
                                  String previewUrl,
                                  String fullUrl,
                                  String downloadUrl,
                                  int sortOrder,
                                  boolean active,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
        this.id = id;
        this.slug = slug;
        this.brandSlug = brandSlug;
        this.brandName = brandName;
        this.title = title;
        this.fileName = fileName;
        this.previewUrl = previewUrl;
        this.fullUrl = fullUrl;
        this.downloadUrl = downloadUrl;
        this.sortOrder = sortOrder;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getBrandSlug() {
        return brandSlug;
    }

    public String getBrandName() {
        return brandName;
    }

    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
