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
