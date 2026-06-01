package com.vehiclewallpaper.backend.admin;

import java.time.LocalDateTime;

public class AdminDashboardResponse {

    private final LocalDateTime catalogGeneratedAt;
    private final long totalBrands;
    private final long totalWallpapers;
    private final long activeWallpapers;
    private final long inactiveWallpapers;
    private final long pendingFeedback;
    private final long approvedFeedback;
    private final long rejectedFeedback;
    private final long featuredFeedback;

    public AdminDashboardResponse(LocalDateTime catalogGeneratedAt,
                                  long totalBrands,
                                  long totalWallpapers,
                                  long activeWallpapers,
                                  long inactiveWallpapers,
                                  long pendingFeedback,
                                  long approvedFeedback,
                                  long rejectedFeedback,
                                  long featuredFeedback) {
        this.catalogGeneratedAt = catalogGeneratedAt;
        this.totalBrands = totalBrands;
        this.totalWallpapers = totalWallpapers;
        this.activeWallpapers = activeWallpapers;
        this.inactiveWallpapers = inactiveWallpapers;
        this.pendingFeedback = pendingFeedback;
        this.approvedFeedback = approvedFeedback;
        this.rejectedFeedback = rejectedFeedback;
        this.featuredFeedback = featuredFeedback;
    }

    public LocalDateTime getCatalogGeneratedAt() {
        return catalogGeneratedAt;
    }

    public long getTotalBrands() {
        return totalBrands;
    }

    public long getTotalWallpapers() {
        return totalWallpapers;
    }

    public long getActiveWallpapers() {
        return activeWallpapers;
    }

    public long getInactiveWallpapers() {
        return inactiveWallpapers;
    }

    public long getPendingFeedback() {
        return pendingFeedback;
    }

    public long getApprovedFeedback() {
        return approvedFeedback;
    }

    public long getRejectedFeedback() {
        return rejectedFeedback;
    }

    public long getFeaturedFeedback() {
        return featuredFeedback;
    }
}
