package com.vehiclewallpaper.backend.admin;

import com.vehiclewallpaper.backend.catalog.BrandRepository;
import com.vehiclewallpaper.backend.catalog.CatalogOverviewResponse;
import com.vehiclewallpaper.backend.catalog.CatalogService;
import com.vehiclewallpaper.backend.catalog.WallpaperEntity;
import com.vehiclewallpaper.backend.catalog.WallpaperRepository;
import com.vehiclewallpaper.backend.feedback.FeedbackMessage;
import com.vehiclewallpaper.backend.feedback.FeedbackRepository;
import com.vehiclewallpaper.backend.feedback.FeedbackStatus;
import com.vehiclewallpaper.backend.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminService {

    private final CatalogService catalogService;
    private final BrandRepository brandRepository;
    private final WallpaperRepository wallpaperRepository;
    private final FeedbackRepository feedbackRepository;

    public AdminService(CatalogService catalogService,
                        BrandRepository brandRepository,
                        WallpaperRepository wallpaperRepository,
                        FeedbackRepository feedbackRepository) {
        this.catalogService = catalogService;
        this.brandRepository = brandRepository;
        this.wallpaperRepository = wallpaperRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        CatalogOverviewResponse overview = catalogService.getOverview();
        List<WallpaperEntity> wallpapers = wallpaperRepository.findAllForAdmin();

        long activeWallpapers = 0;
        for (WallpaperEntity wallpaper : wallpapers) {
            if (wallpaper.isActive()) {
                activeWallpapers++;
            }
        }

        return new AdminDashboardResponse(
            overview.getGeneratedAt(),
            brandRepository.count(),
            wallpapers.size(),
            activeWallpapers,
            wallpapers.size() - activeWallpapers,
            feedbackRepository.countByStatus(FeedbackStatus.PENDING),
            feedbackRepository.countByStatus(FeedbackStatus.APPROVED),
            feedbackRepository.countByStatus(FeedbackStatus.REJECTED),
            feedbackRepository.countByFeaturedTrue()
        );
    }

    @Transactional
    public CatalogOverviewResponse refreshCatalog() {
        return catalogService.refreshCatalog();
    }

    @Transactional(readOnly = true)
    public List<AdminWallpaperResponse> getWallpapers(String brandSlug, Boolean active) {
        List<AdminWallpaperResponse> responses = new ArrayList<AdminWallpaperResponse>();

        for (WallpaperEntity wallpaper : wallpaperRepository.findAllForAdmin()) {
            boolean brandMatches = brandSlug == null || brandSlug.trim().isEmpty()
                || wallpaper.getBrand().getSlug().equalsIgnoreCase(brandSlug.trim());
            boolean activeMatches = active == null || wallpaper.isActive() == active.booleanValue();

            if (brandMatches && activeMatches) {
                responses.add(toWallpaperResponse(wallpaper));
            }
        }

        return responses;
    }

    @Transactional
    public AdminWallpaperResponse updateWallpaper(Long wallpaperId, AdminWallpaperUpdateRequest request) {
        if (request.getTitle() == null && request.getSortOrder() == null && request.getActive() == null) {
            throw new IllegalArgumentException("At least one wallpaper field must be provided.");
        }

        WallpaperEntity wallpaper = wallpaperRepository.findWithBrandById(wallpaperId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallpaper not found: " + wallpaperId));

        if (request.getTitle() != null) {
            String normalizedTitle = request.getTitle().trim();
            if (normalizedTitle.isEmpty()) {
                throw new IllegalArgumentException("Wallpaper title cannot be blank.");
            }
            wallpaper.setTitle(normalizedTitle);
        }

        if (request.getSortOrder() != null) {
            wallpaper.setSortOrder(request.getSortOrder().intValue());
        }

        if (request.getActive() != null) {
            wallpaper.setActive(request.getActive().booleanValue());
        }

        return toWallpaperResponse(wallpaperRepository.save(wallpaper));
    }

    @Transactional(readOnly = true)
    public List<AdminFeedbackResponse> getFeedback(String status, Boolean featured, int limit) {
        FeedbackStatus statusFilter = parseFeedbackStatus(status, true);
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<AdminFeedbackResponse> responses = new ArrayList<AdminFeedbackResponse>();

        for (FeedbackMessage message : feedbackRepository.findAllNewestFirst()) {
            boolean statusMatches = statusFilter == null || message.getStatus() == statusFilter;
            boolean featuredMatches = featured == null || message.isFeatured() == featured.booleanValue();

            if (statusMatches && featuredMatches) {
                responses.add(toFeedbackResponse(message));
            }

            if (responses.size() >= safeLimit) {
                break;
            }
        }

        return responses;
    }

    @Transactional
    public AdminFeedbackResponse updateFeedback(Long feedbackId, AdminFeedbackUpdateRequest request) {
        if (request.getStatus() == null && request.getFeatured() == null) {
            throw new IllegalArgumentException("At least one feedback field must be provided.");
        }

        FeedbackMessage message = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + feedbackId));

        if (request.getStatus() != null) {
            message.setStatus(parseFeedbackStatus(request.getStatus(), false));
        }

        if (request.getFeatured() != null) {
            message.setFeatured(request.getFeatured().booleanValue());
        }

        return toFeedbackResponse(feedbackRepository.save(message));
    }

    private AdminWallpaperResponse toWallpaperResponse(WallpaperEntity wallpaper) {
        return new AdminWallpaperResponse(
            wallpaper.getId(),
            wallpaper.getSlug(),
            wallpaper.getBrand().getSlug(),
            wallpaper.getBrand().getDisplayName(),
            wallpaper.getTitle(),
            wallpaper.getFileName(),
            wallpaper.getPreviewUrl(),
            wallpaper.getFullUrl(),
            wallpaper.getDownloadUrl(),
            wallpaper.getSortOrder(),
            wallpaper.isActive(),
            wallpaper.getCreatedAt(),
            wallpaper.getUpdatedAt()
        );
    }

    private AdminFeedbackResponse toFeedbackResponse(FeedbackMessage message) {
        return new AdminFeedbackResponse(
            message.getId(),
            message.getName(),
            message.getEmail(),
            message.getMessage(),
            message.getStatus().name(),
            message.isFeatured(),
            message.getSourcePage(),
            message.getUserAgent(),
            message.getCreatedAt()
        );
    }

    private FeedbackStatus parseFeedbackStatus(String rawStatus, boolean allowNull) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            if (allowNull) {
                return null;
            }
            throw new IllegalArgumentException("Feedback status cannot be blank.");
        }

        try {
            return FeedbackStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported feedback status: " + rawStatus);
        }
    }
}
