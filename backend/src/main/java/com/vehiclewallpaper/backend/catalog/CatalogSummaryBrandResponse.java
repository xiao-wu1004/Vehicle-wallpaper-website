package com.vehiclewallpaper.backend.catalog;

public class CatalogSummaryBrandResponse {

    private final String slug;
    private final String displayName;
    private final int wallpaperCount;
    private final String coverImageUrl;

    public CatalogSummaryBrandResponse(String slug,
                                       String displayName,
                                       int wallpaperCount,
                                       String coverImageUrl) {
        this.slug = slug;
        this.displayName = displayName;
        this.wallpaperCount = wallpaperCount;
        this.coverImageUrl = coverImageUrl;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getWallpaperCount() {
        return wallpaperCount;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }
}
