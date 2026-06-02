package com.vehiclewallpaper.backend.catalog;

public class WallpaperInteractionResponse {

    private final String wallpaperId;
    private final boolean favorited;
    private final long favoriteCount;
    private final long downloadCount;
    private final double hotScore;

    public WallpaperInteractionResponse(String wallpaperId,
                                        boolean favorited,
                                        long favoriteCount,
                                        long downloadCount,
                                        double hotScore) {
        this.wallpaperId = wallpaperId;
        this.favorited = favorited;
        this.favoriteCount = favoriteCount;
        this.downloadCount = downloadCount;
        this.hotScore = hotScore;
    }

    public String getWallpaperId() {
        return wallpaperId;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public long getFavoriteCount() {
        return favoriteCount;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public double getHotScore() {
        return hotScore;
    }
}
