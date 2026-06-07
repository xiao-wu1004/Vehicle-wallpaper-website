package com.vehiclewallpaper.backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WallpaperFavoriteRepository extends JpaRepository<WallpaperFavoriteEntity, Long> {

    Optional<WallpaperFavoriteEntity> findByVisitorKeyAndWallpaperId(String visitorKey, Long wallpaperId);

    List<WallpaperFavoriteEntity> findAllByVisitorKeyOrderByCreatedAtDesc(String visitorKey);

    @Query("select favorite from WallpaperFavoriteEntity favorite "
        + "join fetch favorite.wallpaper wallpaper "
        + "join fetch wallpaper.brand brand "
        + "where favorite.visitorKey = :visitorKey order by favorite.createdAt desc")
    List<WallpaperFavoriteEntity> findRecentByVisitorKeyWithWallpaper(@Param("visitorKey") String visitorKey,
                                                                      Pageable pageable);

    List<WallpaperFavoriteEntity> findAllByWallpaperBrandId(Long brandId);

    long countByVisitorKey(String visitorKey);

    @Query("select favorite.wallpaper.id, count(favorite.id) from WallpaperFavoriteEntity favorite "
        + "where favorite.wallpaper.id in :wallpaperIds group by favorite.wallpaper.id")
    List<Object[]> countByWallpaperIds(@Param("wallpaperIds") List<Long> wallpaperIds);

    @Query("select favorite.wallpaper.id from WallpaperFavoriteEntity favorite where favorite.visitorKey = :visitorKey")
    List<Long> findFavoriteWallpaperIdsByVisitorKey(@Param("visitorKey") String visitorKey);

    @Query("select favorite.wallpaper.id from WallpaperFavoriteEntity favorite "
        + "where favorite.visitorKey = :visitorKey and favorite.wallpaper.id in :wallpaperIds")
    List<Long> findFavoriteWallpaperIdsByVisitorKeyAndWallpaperIds(@Param("visitorKey") String visitorKey,
                                                                   @Param("wallpaperIds") List<Long> wallpaperIds);

    @Modifying(clearAutomatically = true)
    @Query("delete from WallpaperFavoriteEntity favorite where favorite.wallpaper.id = :wallpaperId")
    int deleteByWallpaperId(@Param("wallpaperId") Long wallpaperId);

    @Modifying(clearAutomatically = true)
    @Query("delete from WallpaperFavoriteEntity favorite where favorite.wallpaper.id in "
        + "(select wallpaper.id from WallpaperEntity wallpaper where wallpaper.brand.id = :brandId)")
    int deleteByBrandId(@Param("brandId") Long brandId);
}
