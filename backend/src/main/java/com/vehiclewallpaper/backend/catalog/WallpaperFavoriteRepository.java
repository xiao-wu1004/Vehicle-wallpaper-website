package com.vehiclewallpaper.backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WallpaperFavoriteRepository extends JpaRepository<WallpaperFavoriteEntity, Long> {

    Optional<WallpaperFavoriteEntity> findByVisitorKeyAndWallpaperId(String visitorKey, Long wallpaperId);

    List<WallpaperFavoriteEntity> findAllByVisitorKeyOrderByCreatedAtDesc(String visitorKey);

    List<WallpaperFavoriteEntity> findAllByWallpaperBrandId(Long brandId);

    long countByVisitorKey(String visitorKey);

    @Query("select favorite.wallpaper.id, count(favorite.id) from WallpaperFavoriteEntity favorite "
        + "where favorite.wallpaper.id in :wallpaperIds group by favorite.wallpaper.id")
    List<Object[]> countByWallpaperIds(@Param("wallpaperIds") List<Long> wallpaperIds);

    @Query("select favorite.wallpaper.id from WallpaperFavoriteEntity favorite where favorite.visitorKey = :visitorKey")
    List<Long> findFavoriteWallpaperIdsByVisitorKey(@Param("visitorKey") String visitorKey);

    @Modifying(clearAutomatically = true)
    @Query("delete from WallpaperFavoriteEntity favorite where favorite.wallpaper.id = :wallpaperId")
    int deleteByWallpaperId(@Param("wallpaperId") Long wallpaperId);

    @Modifying(clearAutomatically = true)
    @Query("delete from WallpaperFavoriteEntity favorite where favorite.wallpaper.brand.id = :brandId")
    int deleteByBrandId(@Param("brandId") Long brandId);
}
