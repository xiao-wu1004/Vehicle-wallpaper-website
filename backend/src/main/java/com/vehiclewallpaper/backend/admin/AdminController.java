package com.vehiclewallpaper.backend.admin;

import com.vehiclewallpaper.backend.catalog.CatalogOverviewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/brands")
    public List<AdminBrandResponse> brands() {
        return adminService.getBrands();
    }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminBrandResponse createBrand(@Valid @RequestBody AdminBrandUpsertRequest request) {
        return adminService.createBrand(request);
    }

    @PatchMapping("/brands/{brandId}")
    public AdminBrandResponse updateBrand(@PathVariable Long brandId,
                                          @Valid @RequestBody AdminBrandUpsertRequest request) {
        return adminService.updateBrand(brandId, request);
    }

    @DeleteMapping("/brands/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBrand(@PathVariable Long brandId) {
        adminService.deleteBrand(brandId);
    }

    @GetMapping("/wallpapers")
    public List<AdminWallpaperResponse> wallpapers(@RequestParam(required = false) String brand,
                                                   @RequestParam(required = false) Boolean active) {
        return adminService.getWallpapers(brand, active);
    }

    @PostMapping(value = "/brands/{brandId}/wallpapers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminWallpaperResponse uploadWallpaper(@PathVariable Long brandId,
                                                  @RequestParam(required = false) String title,
                                                  @RequestParam(required = false) Integer sortOrder,
                                                  @RequestParam(required = false) Boolean active,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "previewFile", required = false) MultipartFile previewFile) {
        return adminService.uploadWallpaper(brandId, title, sortOrder, active, file, previewFile);
    }

    @PatchMapping("/wallpapers/{wallpaperId}")
    public AdminWallpaperResponse updateWallpaper(@PathVariable Long wallpaperId,
                                                  @Valid @RequestBody AdminWallpaperUpdateRequest request) {
        return adminService.updateWallpaper(wallpaperId, request);
    }

    @DeleteMapping("/wallpapers/{wallpaperId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWallpaper(@PathVariable Long wallpaperId) {
        adminService.deleteWallpaper(wallpaperId);
    }

    @GetMapping("/feedback")
    public List<AdminFeedbackResponse> feedback(@RequestParam(required = false) String status,
                                                @RequestParam(required = false) Boolean featured,
                                                @RequestParam(defaultValue = "50") int limit) {
        return adminService.getFeedback(status, featured, limit);
    }

    @GetMapping("/logs")
    public List<AdminOperationLogResponse> logs(@RequestParam(defaultValue = "50") int limit) {
        return adminService.getOperationLogs(limit);
    }

    @PatchMapping("/feedback/{feedbackId}")
    public AdminFeedbackResponse updateFeedback(@PathVariable Long feedbackId,
                                                @RequestBody AdminFeedbackUpdateRequest request) {
        return adminService.updateFeedback(feedbackId, request);
    }
}
