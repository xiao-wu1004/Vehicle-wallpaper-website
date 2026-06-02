package com.vehiclewallpaper.backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<BrandEntity, Long> {

    List<BrandEntity> findAllByOrderBySortOrderAsc();

    Optional<BrandEntity> findBySlugIgnoreCase(String slug);

    Optional<BrandEntity> findByFolderNameIgnoreCase(String folderName);
}
