package com.vehiclewallpaper.backend.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public CatalogOverviewResponse overview(@RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey) {
        return catalogService.getOverview(visitorKey);
    }

    @GetMapping("/brands")
    public List<BrandCatalogResponse> brands(@RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey) {
        return catalogService.getBrands(visitorKey);
    }

    @GetMapping("/brands/{brandSlug}")
    public BrandCatalogResponse brand(@PathVariable String brandSlug,
                                      @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey) {
        return catalogService.getBrand(brandSlug, visitorKey);
    }

    @GetMapping("/highlights")
    public List<WallpaperResponse> highlights(@RequestParam(defaultValue = "6") int limit,
                                              @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey) {
        return catalogService.search(null, null, "hot", Math.max(1, Math.min(limit, 12)), false, visitorKey);
    }

    @GetMapping("/wallpapers")
    public List<WallpaperResponse> wallpapers(@RequestParam(required = false) String brand,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(required = false) String sort,
                                              @RequestParam(defaultValue = "false") boolean favoritesOnly,
                                              @RequestParam(defaultValue = "24") int limit,
                                              @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey) {
        return catalogService.search(brand, q, sort, Math.max(1, Math.min(limit, 120)), favoritesOnly, visitorKey);
    }

    @GetMapping("/me")
    public CatalogProfileResponse profile(@RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey,
                                          @RequestParam(defaultValue = "8") int favoriteLimit,
                                          @RequestParam(defaultValue = "8") int downloadLimit) {
        return catalogService.getProfile(
            visitorKey,
            Math.max(1, Math.min(favoriteLimit, 24)),
            Math.max(1, Math.min(downloadLimit, 24))
        );
    }

    @PostMapping("/wallpapers/{wallpaperSlug}/favorite")
    public WallpaperInteractionResponse favorite(@PathVariable String wallpaperSlug,
                                                 @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey) {
        return catalogService.addFavorite(wallpaperSlug, visitorKey);
    }

    @DeleteMapping("/wallpapers/{wallpaperSlug}/favorite")
    public WallpaperInteractionResponse unfavorite(@PathVariable String wallpaperSlug,
                                                   @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey) {
        return catalogService.removeFavorite(wallpaperSlug, visitorKey);
    }

    @PostMapping("/wallpapers/{wallpaperSlug}/downloads")
    public WallpaperInteractionResponse download(@PathVariable String wallpaperSlug,
                                                 @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey) {
        return catalogService.recordDownload(wallpaperSlug, visitorKey);
    }
}
