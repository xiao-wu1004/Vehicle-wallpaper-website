package com.vehiclewallpaper.backend.catalog;

import java.util.List;

public class CatalogProfileResponse {

    private final String visitorKey;
    private final long favoriteCount;
    private final long downloadCount;
    private final List<WallpaperResponse> favorites;
    private final List<WallpaperResponse> recentDownloads;

    public CatalogProfileResponse(String visitorKey,
                                  long favoriteCount,
                                  long downloadCount,
                                  List<WallpaperResponse> favorites,
                                  List<WallpaperResponse> recentDownloads) {
        this.visitorKey = visitorKey;
        this.favoriteCount = favoriteCount;
        this.downloadCount = downloadCount;
        this.favorites = favorites;
        this.recentDownloads = recentDownloads;
    }

    public String getVisitorKey() {
        return visitorKey;
    }

    public long getFavoriteCount() {
        return favoriteCount;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public List<WallpaperResponse> getFavorites() {
        return favorites;
    }

    public List<WallpaperResponse> getRecentDownloads() {
        return recentDownloads;
    }
}
