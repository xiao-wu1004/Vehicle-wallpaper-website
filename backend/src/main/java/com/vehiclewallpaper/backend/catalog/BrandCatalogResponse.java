package com.vehiclewallpaper.backend.catalog;

import java.util.List;

public class BrandCatalogResponse {

    private final String slug;
    private final String name;
    private final String folderName;
    private final int wallpaperCount;
    private final String coverImageUrl;
    private final List<WallpaperResponse> wallpapers;

    public BrandCatalogResponse(String slug,
                                String name,
                                String folderName,
                                int wallpaperCount,
                                String coverImageUrl,
                                List<WallpaperResponse> wallpapers) {
        this.slug = slug;
        this.name = name;
        this.folderName = folderName;
        this.wallpaperCount = wallpaperCount;
        this.coverImageUrl = coverImageUrl;
        this.wallpapers = wallpapers;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getFolderName() {
        return folderName;
    }

    public int getWallpaperCount() {
        return wallpaperCount;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public List<WallpaperResponse> getWallpapers() {
        return wallpapers;
    }
}
