package com.vehiclewallpaper.backend.catalog;

import java.time.LocalDateTime;
import java.util.List;

public class CatalogOverviewResponse {

    private final LocalDateTime generatedAt;
    private final int totalBrands;
    private final int totalWallpapers;
    private final List<BrandCatalogResponse> brands;

    public CatalogOverviewResponse(LocalDateTime generatedAt,
                                   int totalBrands,
                                   int totalWallpapers,
                                   List<BrandCatalogResponse> brands) {
        this.generatedAt = generatedAt;
        this.totalBrands = totalBrands;
        this.totalWallpapers = totalWallpapers;
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

    public List<BrandCatalogResponse> getBrands() {
        return brands;
    }
}
