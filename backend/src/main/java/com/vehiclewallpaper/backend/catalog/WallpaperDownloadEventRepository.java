package com.vehiclewallpaper.backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WallpaperDownloadEventRepository extends JpaRepository<WallpaperDownloadEventEntity, Long> {

    List<WallpaperDownloadEventEntity> findAllByVisitorKeyOrderByCreatedAtDesc(String visitorKey);

    @Query("select download from WallpaperDownloadEventEntity download "
        + "join fetch download.wallpaper wallpaper "
        + "join fetch wallpaper.brand brand "
        + "where download.visitorKey = :visitorKey order by download.createdAt desc")
    List<WallpaperDownloadEventEntity> findRecentByVisitorKeyWithWallpaper(@Param("visitorKey") String visitorKey,
                                                                           Pageable pageable);

    List<WallpaperDownloadEventEntity> findAllByWallpaperBrandId(Long brandId);

    long countByVisitorKey(String visitorKey);

    @Query("select download.wallpaper.id, count(download.id) from WallpaperDownloadEventEntity download "
        + "where download.wallpaper.id in :wallpaperIds group by download.wallpaper.id")
    List<Object[]> countByWallpaperIds(@Param("wallpaperIds") List<Long> wallpaperIds);

    @Modifying(clearAutomatically = true)
    @Query("delete from WallpaperDownloadEventEntity download where download.wallpaper.id = :wallpaperId")
    int deleteByWallpaperId(@Param("wallpaperId") Long wallpaperId);

    @Modifying(clearAutomatically = true)
    @Query("delete from WallpaperDownloadEventEntity download where download.wallpaper.id in "
        + "(select wallpaper.id from WallpaperEntity wallpaper where wallpaper.brand.id = :brandId)")
    int deleteByBrandId(@Param("brandId") Long brandId);
}
