package com.vehiclewallpaper.backend.catalog;

import java.time.LocalDateTime;
import java.util.List;

public class CatalogSummaryResponse {

    private final LocalDateTime generatedAt;
    private final int totalBrands;
    private final int totalWallpapers;
    private final long totalFavorites;
    private final long totalDownloads;
    private final List<CatalogSummaryBrandResponse> brands;
    private final List<WallpaperResponse> trendingWallpapers;

    public CatalogSummaryResponse(LocalDateTime generatedAt,
                                  int totalBrands,
                                  int totalWallpapers,
                                  long totalFavorites,
                                  long totalDownloads,
                                  List<CatalogSummaryBrandResponse> brands,
                                  List<WallpaperResponse> trendingWallpapers) {
        this.generatedAt = generatedAt;
        this.totalBrands = totalBrands;
        this.totalWallpapers = totalWallpapers;
        this.totalFavorites = totalFavorites;
        this.totalDownloads = totalDownloads;
        this.brands = brands;
        this.trendingWallpapers = trendingWallpapers;
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

    public List<CatalogSummaryBrandResponse> getBrands() {
        return brands;
    }

    public List<WallpaperResponse> getTrendingWallpapers() {
        return trendingWallpapers;
    }
}
