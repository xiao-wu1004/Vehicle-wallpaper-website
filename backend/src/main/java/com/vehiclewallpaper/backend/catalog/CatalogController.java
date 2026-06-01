package com.vehiclewallpaper.backend.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public CatalogOverviewResponse overview() {
        return catalogService.getOverview();
    }

    @GetMapping("/brands")
    public List<BrandCatalogResponse> brands() {
        return catalogService.getBrands();
    }

    @GetMapping("/brands/{brandSlug}")
    public BrandCatalogResponse brand(@PathVariable String brandSlug) {
        return catalogService.getBrand(brandSlug);
    }

    @GetMapping("/highlights")
    public List<WallpaperResponse> highlights(@RequestParam(defaultValue = "6") int limit) {
        return catalogService.getHighlights(Math.max(1, Math.min(limit, 12)));
    }

    @GetMapping("/wallpapers")
    public List<WallpaperResponse> wallpapers(@RequestParam(required = false) String brand,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(defaultValue = "24") int limit) {
        return catalogService.search(brand, q, Math.max(1, Math.min(limit, 60)));
    }
}
