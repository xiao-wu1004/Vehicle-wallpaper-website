package com.vehiclewallpaper.backend.catalog;

import com.vehiclewallpaper.backend.user.UserAuthenticationService;
import com.vehiclewallpaper.backend.user.UserIdentity;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

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
    private final UserAuthenticationService userAuthenticationService;

    public CatalogController(CatalogService catalogService,
                             UserAuthenticationService userAuthenticationService) {
        this.catalogService = catalogService;
        this.userAuthenticationService = userAuthenticationService;
    }

    @GetMapping
    public CatalogOverviewResponse overview(@RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey,
                                            HttpServletRequest request) {
        return catalogService.getOverview(resolveEngagementKey(request, visitorKey));
    }

    @GetMapping("/brands")
    public List<BrandCatalogResponse> brands(@RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey,
                                             HttpServletRequest request) {
        return catalogService.getBrands(resolveEngagementKey(request, visitorKey));
    }

    @GetMapping("/brands/{brandSlug}")
    public BrandCatalogResponse brand(@PathVariable String brandSlug,
                                      @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey,
                                      HttpServletRequest request) {
        return catalogService.getBrand(brandSlug, resolveEngagementKey(request, visitorKey));
    }

    @GetMapping("/highlights")
    public List<WallpaperResponse> highlights(@RequestParam(defaultValue = "6") int limit,
                                              @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey,
                                              HttpServletRequest request) {
        return catalogService.search(null, null, "hot", Math.max(1, Math.min(limit, 12)), false, resolveEngagementKey(request, visitorKey));
    }

    @GetMapping("/wallpapers")
    public List<WallpaperResponse> wallpapers(@RequestParam(required = false) String brand,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(required = false) String sort,
                                              @RequestParam(defaultValue = "false") boolean favoritesOnly,
                                              @RequestParam(defaultValue = "24") int limit,
                                              @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey,
                                              HttpServletRequest request) {
        return catalogService.search(
            brand,
            q,
            sort,
            Math.max(1, Math.min(limit, 120)),
            favoritesOnly,
            resolveEngagementKey(request, visitorKey)
        );
    }

    @GetMapping("/me")
    public CatalogProfileResponse profile(@RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey,
                                          @RequestParam(defaultValue = "8") int favoriteLimit,
                                          @RequestParam(defaultValue = "8") int downloadLimit,
                                          HttpServletRequest request) {
        UserIdentity identity = userAuthenticationService.authenticateOptional(request);
        String engagementKey = identity == null ? visitorKey : identity.getPublicKey();
        return catalogService.getProfile(
            engagementKey,
            Math.max(1, Math.min(favoriteLimit, 24)),
            Math.max(1, Math.min(downloadLimit, 24)),
            identity != null,
            identity == null ? "" : identity.getDisplayName(),
            identity == null ? "" : identity.getEmail()
        );
    }

    @PostMapping("/wallpapers/{wallpaperSlug}/favorite")
    public WallpaperInteractionResponse favorite(@PathVariable String wallpaperSlug,
                                                 @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey,
                                                 HttpServletRequest request) {
        return catalogService.addFavorite(wallpaperSlug, resolveEngagementKey(request, visitorKey));
    }

    @DeleteMapping("/wallpapers/{wallpaperSlug}/favorite")
    public WallpaperInteractionResponse unfavorite(@PathVariable String wallpaperSlug,
                                                   @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey,
                                                   HttpServletRequest request) {
        return catalogService.removeFavorite(wallpaperSlug, resolveEngagementKey(request, visitorKey));
    }

    @PostMapping("/wallpapers/{wallpaperSlug}/downloads")
    public WallpaperInteractionResponse download(@PathVariable String wallpaperSlug,
                                                 @RequestHeader(value = "X-Visitor-Key", required = false) String visitorKey,
                                                 HttpServletRequest request) {
        return catalogService.recordDownload(wallpaperSlug, resolveEngagementKey(request, visitorKey));
    }

    private String resolveEngagementKey(HttpServletRequest request, String visitorKey) {
        UserIdentity identity = userAuthenticationService.authenticateOptional(request);
        return identity == null ? visitorKey : identity.getPublicKey();
    }
}
