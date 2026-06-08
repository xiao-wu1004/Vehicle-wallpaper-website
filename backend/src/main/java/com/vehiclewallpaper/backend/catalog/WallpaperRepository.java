package com.vehiclewallpaper.backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WallpaperRepository extends JpaRepository<WallpaperEntity, Long> {

    @Query("select wallpaper from WallpaperEntity wallpaper join fetch wallpaper.brand brand "
        + "where brand.id = :brandId order by wallpaper.sortOrder asc, wallpaper.id asc")
    List<WallpaperEntity> findByBrandIdOrderBySortOrderAsc(@Param("brandId") Long brandId);

    @Query("select wallpaper from WallpaperEntity wallpaper join fetch wallpaper.brand brand "
        + "where wallpaper.active = true order by brand.sortOrder asc, wallpaper.sortOrder asc, wallpaper.id asc")
    List<WallpaperEntity> findAllActiveWithBrandOrder();

    @Query("select wallpaper from WallpaperEntity wallpaper join fetch wallpaper.brand brand "
        + "where wallpaper.active = true and lower(brand.slug) = lower(:brandSlug) "
        + "order by brand.sortOrder asc, wallpaper.sortOrder asc, wallpaper.id asc")
    List<WallpaperEntity> findAllActiveByBrandSlugWithBrandOrder(@Param("brandSlug") String brandSlug);

    @Query("select wallpaper from WallpaperEntity wallpaper join fetch wallpaper.brand brand "
        + "where wallpaper.active = true and ("
        + "lower(wallpaper.title) like :query "
        + "or lower(wallpaper.fileName) like :query "
        + "or lower(wallpaper.slug) like :query "
        + "or lower(brand.slug) like :query "
        + "or lower(brand.displayName) like :query) "
        + "order by brand.sortOrder asc, wallpaper.sortOrder asc, wallpaper.id asc")
    List<WallpaperEntity> searchActiveWithBrandOrder(@Param("query") String query);

    @Query("select wallpaper from WallpaperEntity wallpaper join fetch wallpaper.brand brand "
        + "where wallpaper.active = true and lower(brand.slug) = lower(:brandSlug) and ("
        + "lower(wallpaper.title) like :query "
        + "or lower(wallpaper.fileName) like :query "
        + "or lower(wallpaper.slug) like :query "
        + "or lower(brand.slug) like :query "
        + "or lower(brand.displayName) like :query) "
        + "order by brand.sortOrder asc, wallpaper.sortOrder asc, wallpaper.id asc")
    List<WallpaperEntity> searchActiveByBrandSlugWithBrandOrder(@Param("brandSlug") String brandSlug,
                                                                @Param("query") String query);

    @Query("select wallpaper from WallpaperEntity wallpaper join fetch wallpaper.brand brand "
        + "where wallpaper.active = true and wallpaper.id in :wallpaperIds")
    List<WallpaperEntity> findAllActiveByIdsWithBrand(@Param("wallpaperIds") Collection<Long> wallpaperIds);

    @Query("select wallpaper from WallpaperEntity wallpaper join fetch wallpaper.brand brand "
        + "order by brand.sortOrder asc, wallpaper.sortOrder asc, wallpaper.id asc")
    List<WallpaperEntity> findAllForAdmin();

    @Query("select wallpaper from WallpaperEntity wallpaper join fetch wallpaper.brand brand where wallpaper.id = :id")
    Optional<WallpaperEntity> findWithBrandById(@Param("id") Long id);

    @Query("select wallpaper from WallpaperEntity wallpaper join fetch wallpaper.brand brand where lower(wallpaper.slug) = lower(:slug)")
    Optional<WallpaperEntity> findWithBrandBySlug(@Param("slug") String slug);

    boolean existsBySlugIgnoreCase(String slug);

    @Modifying(clearAutomatically = true)
    @Query("delete from WallpaperEntity wallpaper where wallpaper.brand.id = :brandId")
    int deleteByBrandId(@Param("brandId") Long brandId);
}
