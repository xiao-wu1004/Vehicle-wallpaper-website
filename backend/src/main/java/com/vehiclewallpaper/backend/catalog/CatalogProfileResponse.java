package com.vehiclewallpaper.backend.catalog;

import java.util.List;

public class CatalogProfileResponse {

    private final String visitorKey;
    private final boolean authenticated;
    private final String displayName;
    private final String email;
    private final long favoriteCount;
    private final long downloadCount;
    private final List<WallpaperResponse> favorites;
    private final List<WallpaperResponse> recentDownloads;

    public CatalogProfileResponse(String visitorKey,
                                  boolean authenticated,
                                  String displayName,
                                  String email,
                                  long favoriteCount,
                                  long downloadCount,
                                  List<WallpaperResponse> favorites,
                                  List<WallpaperResponse> recentDownloads) {
        this.visitorKey = visitorKey;
        this.authenticated = authenticated;
        this.displayName = displayName;
        this.email = email;
        this.favoriteCount = favoriteCount;
        this.downloadCount = downloadCount;
        this.favorites = favorites;
        this.recentDownloads = recentDownloads;
    }

    public String getVisitorKey() {
        return visitorKey;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
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
