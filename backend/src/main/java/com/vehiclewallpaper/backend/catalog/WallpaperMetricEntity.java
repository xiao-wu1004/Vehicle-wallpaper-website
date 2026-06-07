package com.vehiclewallpaper.backend.catalog;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallpaper_metrics")
public class WallpaperMetricEntity {

    @Id
    @Column(name = "wallpaper_id", nullable = false)
    private Long wallpaperId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallpaper_id", nullable = false)
    private WallpaperEntity wallpaper;

    @Column(name = "favorite_count", nullable = false)
    private long favoriteCount;

    @Column(name = "download_count", nullable = false)
    private long downloadCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getWallpaperId() {
        return wallpaperId;
    }

    public void setWallpaperId(Long wallpaperId) {
        this.wallpaperId = wallpaperId;
    }

    public WallpaperEntity getWallpaper() {
        return wallpaper;
    }

    public void setWallpaper(WallpaperEntity wallpaper) {
        this.wallpaper = wallpaper;
    }

    public long getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(long favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
