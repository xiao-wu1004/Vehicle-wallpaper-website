package com.vehiclewallpaper.backend.catalog;

public class WallpaperResponse {

    private final String id;
    private final Long wallpaperId;
    private final String brandSlug;
    private final String title;
    private final String fileName;
    private final String previewUrl;
    private final String fullUrl;
    private final String downloadUrl;
    private final int sortOrder;
    private final long favoriteCount;
    private final long downloadCount;
    private final double hotScore;
    private final boolean favorited;
    private final java.time.LocalDateTime createdAt;

    public WallpaperResponse(String id,
                             Long wallpaperId,
                             String brandSlug,
                             String title,
                             String fileName,
                             String previewUrl,
                             String fullUrl,
                             String downloadUrl,
                             int sortOrder,
                             long favoriteCount,
                             long downloadCount,
                             double hotScore,
                             boolean favorited,
                             java.time.LocalDateTime createdAt) {
        this.id = id;
        this.wallpaperId = wallpaperId;
        this.brandSlug = brandSlug;
        this.title = title;
        this.fileName = fileName;
        this.previewUrl = previewUrl;
        this.fullUrl = fullUrl;
        this.downloadUrl = downloadUrl;
        this.sortOrder = sortOrder;
        this.favoriteCount = favoriteCount;
        this.downloadCount = downloadCount;
        this.hotScore = hotScore;
        this.favorited = favorited;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public Long getWallpaperId() {
        return wallpaperId;
    }

    public String getBrandSlug() {
        return brandSlug;
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

    public long getFavoriteCount() {
        return favoriteCount;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public double getHotScore() {
        return hotScore;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
