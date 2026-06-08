package com.vehiclewallpaper.backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WallpaperMetricRepository extends JpaRepository<WallpaperMetricEntity, Long> {

    Optional<WallpaperMetricEntity> findByWallpaperId(Long wallpaperId);

    boolean existsByWallpaperId(Long wallpaperId);

    List<WallpaperMetricEntity> findAllByWallpaperIdIn(Collection<Long> wallpaperIds);

    @Query("select coalesce(sum(metric.favoriteCount), 0) from WallpaperMetricEntity metric")
    long sumFavoriteCount();

    @Query("select coalesce(sum(metric.downloadCount), 0) from WallpaperMetricEntity metric")
    long sumDownloadCount();

    @Query(value = "select metric.wallpaper_id from wallpaper_metrics metric "
        + "join wallpapers wallpaper on wallpaper.id = metric.wallpaper_id "
        + "where wallpaper.active = true "
        + "order by (metric.favorite_count * 4.0 + metric.download_count * 1.5) desc, "
        + "metric.download_count desc, metric.favorite_count desc, wallpaper.created_at desc, wallpaper.id asc "
        + "limit :limit", nativeQuery = true)
    List<Long> findTopActiveWallpaperIds(@Param("limit") int limit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update wallpaper_metrics "
        + "set favorite_count = case "
        + "when favorite_count + :delta < 0 then 0 "
        + "else favorite_count + :delta end, "
        + "updated_at = :updatedAt "
        + "where wallpaper_id = :wallpaperId", nativeQuery = true)
    int adjustFavoriteCount(@Param("wallpaperId") Long wallpaperId,
                            @Param("delta") long delta,
                            @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update wallpaper_metrics "
        + "set download_count = case "
        + "when download_count + :delta < 0 then 0 "
        + "else download_count + :delta end, "
        + "updated_at = :updatedAt "
        + "where wallpaper_id = :wallpaperId", nativeQuery = true)
    int adjustDownloadCount(@Param("wallpaperId") Long wallpaperId,
                            @Param("delta") long delta,
                            @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(clearAutomatically = true)
    @Query("delete from WallpaperMetricEntity metric where metric.wallpaper.id = :wallpaperId")
    int deleteByWallpaperId(@Param("wallpaperId") Long wallpaperId);

    @Modifying(clearAutomatically = true)
    @Query(value = "delete from wallpaper_metrics where wallpaper_id in "
        + "(select id from wallpapers where brand_id = :brandId)", nativeQuery = true)
    int deleteByBrandId(@Param("brandId") Long brandId);
}
