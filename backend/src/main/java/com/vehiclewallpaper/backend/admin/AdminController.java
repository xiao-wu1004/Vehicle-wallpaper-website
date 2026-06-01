package com.vehiclewallpaper.backend.admin;

import com.vehiclewallpaper.backend.catalog.CatalogOverviewResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.getDashboard();
    }

    @PostMapping("/catalog/refresh")
    public CatalogOverviewResponse refreshCatalog() {
        return adminService.refreshCatalog();
    }

    @GetMapping("/wallpapers")
    public List<AdminWallpaperResponse> wallpapers(@RequestParam(required = false) String brand,
                                                   @RequestParam(required = false) Boolean active) {
        return adminService.getWallpapers(brand, active);
    }

    @PatchMapping("/wallpapers/{wallpaperId}")
    public AdminWallpaperResponse updateWallpaper(@PathVariable Long wallpaperId,
                                                  @Valid @RequestBody AdminWallpaperUpdateRequest request) {
        return adminService.updateWallpaper(wallpaperId, request);
    }

    @GetMapping("/feedback")
    public List<AdminFeedbackResponse> feedback(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) Boolean featured,
                                                @RequestParam(defaultValue = "50") int limit) {
        return adminService.getFeedback(status, featured, limit);
    }

    @PatchMapping("/feedback/{feedbackId}")
    public AdminFeedbackResponse updateFeedback(@PathVariable Long feedbackId,
                                                @RequestBody AdminFeedbackUpdateRequest request) {
        return adminService.updateFeedback(feedbackId, request);
    }
}
