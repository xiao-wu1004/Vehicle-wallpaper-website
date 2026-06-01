package com.vehiclewallpaper.backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WallpaperRepository extends JpaRepository<WallpaperEntity, Long> {

    List<WallpaperEntity> findByBrandIdOrderBySortOrderAsc(Long brandId);

    @Modifying
    @Query("delete from WallpaperEntity wallpaper where wallpaper.brand.id = :brandId")
    int deleteByBrandId(@Param("brandId") Long brandId);
}
