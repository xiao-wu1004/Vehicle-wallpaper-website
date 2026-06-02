package com.vehiclewallpaper.backend.catalog;

import java.time.LocalDateTime;
import java.util.List;

public class CatalogOverviewResponse {

    private final LocalDateTime generatedAt;
    private final int totalBrands;
    private final int totalWallpapers;
    private final long totalFavorites;
    private final long totalDownloads;
    private final List<WallpaperResponse> trendingWallpapers;
    private final List<BrandCatalogResponse> brands;

    public CatalogOverviewResponse(LocalDateTime generatedAt,
                                   int totalBrands,
                                   int totalWallpapers,
                                   long totalFavorites,
                                   long totalDownloads,
                                   List<WallpaperResponse> trendingWallpapers,
                                   List<BrandCatalogResponse> brands) {
        this.generatedAt = generatedAt;
        this.totalBrands = totalBrands;
        this.totalWallpapers = totalWallpapers;
        this.totalFavorites = totalFavorites;
        this.totalDownloads = totalDownloads;
        this.trendingWallpapers = trendingWallpapers;
        this.brands = brands;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public int getTotalBrands() {
        return totalBrands;
    }

    public int getTotalWallpapers() {
        return totalWallpapers;
    }

    public long getTotalFavorites() {
        return totalFavorites;
    }

    public long getTotalDownloads() {
        return totalDownloads;
    }

    public List<WallpaperResponse> getTrendingWallpapers() {
        return trendingWallpapers;
    }

    public List<BrandCatalogResponse> getBrands() {
        return brands;
    }
}
