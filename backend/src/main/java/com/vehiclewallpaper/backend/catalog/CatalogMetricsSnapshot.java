package com.vehiclewallpaper.backend.catalog;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class CatalogMetricsSnapshot {

    private final long totalFavorites;
    private final long totalDownloads;
    private final Map<Long, Long> favoriteCounts;
    private final Map<Long, Long> downloadCounts;
    private final Set<Long> favoriteWallpaperIds;

    public CatalogMetricsSnapshot(long totalFavorites,
                                  long totalDownloads,
                                  Map<Long, Long> favoriteCounts,
                                  Map<Long, Long> downloadCounts,
                                  Set<Long> favoriteWallpaperIds) {
        this.totalFavorites = totalFavorites;
        this.totalDownloads = totalDownloads;
        this.favoriteCounts = favoriteCounts == null ? Collections.<Long, Long>emptyMap() : favoriteCounts;
        this.downloadCounts = downloadCounts == null ? Collections.<Long, Long>emptyMap() : downloadCounts;
        this.favoriteWallpaperIds = favoriteWallpaperIds == null ? Collections.<Long>emptySet() : favoriteWallpaperIds;
    }

    public long getTotalFavorites() {
        return totalFavorites;
    }

    public long getTotalDownloads() {
        return totalDownloads;
    }

    public long favoriteCountFor(Long wallpaperId) {
        return favoriteCounts.containsKey(wallpaperId) ? favoriteCounts.get(wallpaperId) : 0L;
    }

    public long downloadCountFor(Long wallpaperId) {
        return downloadCounts.containsKey(wallpaperId) ? downloadCounts.get(wallpaperId) : 0L;
    }

    public boolean isFavorited(Long wallpaperId) {
        return favoriteWallpaperIds.contains(wallpaperId);
    }
}
