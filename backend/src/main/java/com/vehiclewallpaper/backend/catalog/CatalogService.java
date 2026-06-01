package com.vehiclewallpaper.backend.catalog;

import com.vehiclewallpaper.backend.config.CatalogProperties;
import com.vehiclewallpaper.backend.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CatalogService {

    private static final List<BrandDefinition> BRAND_DEFINITIONS = Arrays.asList(
        new BrandDefinition("benz", "奔驰", "MercedesBenz"),
        new BrandDefinition("porsche", "保时捷", "Porsche"),
        new BrandDefinition("hongqi", "红旗", "HongQi"),
        new BrandDefinition("xiaomi", "小米", "Xiaomi"),
        new BrandDefinition("bmw", "宝马", "BMW"),
        new BrandDefinition("audi", "奥迪", "Audi"),
        new BrandDefinition("ferrari", "法拉利", "Ferrari"),
        new BrandDefinition("lamborghini", "兰博基尼", "Lamborghini"),
        new BrandDefinition("astonmartin", "阿斯顿马丁", "Aston Martin"),
        new BrandDefinition("maserati", "玛莎拉蒂", "Maserati"),
        new BrandDefinition("bugatti", "布加迪", "Bugatti"),
        new BrandDefinition("ford", "福特", "Ford")
    );

    private static final List<String> ORIGINAL_EXTENSION_PRIORITY = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> PREVIEW_EXTENSION_PRIORITY = Arrays.asList("webp", "jpg", "jpeg", "png");

    private final CatalogProperties catalogProperties;
    private volatile CatalogOverviewResponse cachedOverview;

    public CatalogService(CatalogProperties catalogProperties) {
        this.catalogProperties = catalogProperties;
    }

    @PostConstruct
    public void warmUp() {
        refreshCatalog();
    }

    public CatalogOverviewResponse getOverview() {
        CatalogOverviewResponse overview = cachedOverview;
        return overview == null ? refreshCatalog() : overview;
    }

    public List<BrandCatalogResponse> getBrands() {
        return getOverview().getBrands();
    }

    public BrandCatalogResponse getBrand(String brandSlug) {
        for (BrandCatalogResponse brand : getBrands()) {
            if (brand.getSlug().equalsIgnoreCase(brandSlug)) {
                return brand;
            }
        }

        throw new ResourceNotFoundException("未找到品牌：" + brandSlug);
    }

    public List<WallpaperResponse> search(String brandSlug, String query, int limit) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<WallpaperResponse> matches = new ArrayList<WallpaperResponse>();

        if (brandSlug != null && !brandSlug.trim().isEmpty()) {
            addMatches(matches, getBrand(brandSlug).getWallpapers(), normalizedQuery, limit);
            return matches;
        }

        for (BrandCatalogResponse brand : getBrands()) {
            addMatches(matches, brand.getWallpapers(), normalizedQuery, limit);
            if (matches.size() >= limit) {
                break;
            }
        }

        return matches;
    }

    public List<WallpaperResponse> getHighlights(int limit) {
        List<WallpaperResponse> highlights = new ArrayList<WallpaperResponse>();
        for (BrandCatalogResponse brand : getBrands()) {
            if (!brand.getWallpapers().isEmpty()) {
                highlights.add(brand.getWallpapers().get(0));
            }
            if (highlights.size() >= limit) {
                break;
            }
        }

        return highlights;
    }

    public synchronized CatalogOverviewResponse refreshCatalog() {
        Path catalogRoot = Paths.get(catalogProperties.getRootPath()).toAbsolutePath().normalize();
        List<BrandCatalogResponse> brands = new ArrayList<BrandCatalogResponse>();
        int totalWallpapers = 0;

        for (BrandDefinition definition : BRAND_DEFINITIONS) {
            BrandCatalogResponse brand = loadBrandCatalog(catalogRoot, definition);
            brands.add(brand);
            totalWallpapers += brand.getWallpaperCount();
        }

        cachedOverview = new CatalogOverviewResponse(
            LocalDateTime.now(),
            brands.size(),
            totalWallpapers,
            brands
        );
        return cachedOverview;
    }

    private void addMatches(List<WallpaperResponse> matches, List<WallpaperResponse> candidates, String query, int limit) {
        for (WallpaperResponse wallpaper : candidates) {
            boolean matchesQuery = query.isEmpty()
                || wallpaper.getTitle().toLowerCase(Locale.ROOT).contains(query)
                || wallpaper.getFileName().toLowerCase(Locale.ROOT).contains(query);

            if (matchesQuery) {
                matches.add(wallpaper);
            }

            if (matches.size() >= limit) {
                break;
            }
        }
    }

    private BrandCatalogResponse loadBrandCatalog(Path catalogRoot, BrandDefinition definition) {
        Path originalsDir = catalogRoot.resolve(definition.getFolderName());
        Path previewsDir = catalogRoot.resolve("_thumb").resolve(definition.getFolderName());

        Map<String, Path> originalFiles = selectPreferredFiles(scanImageFiles(originalsDir), ORIGINAL_EXTENSION_PRIORITY);
        Map<String, Path> previewFiles = selectPreferredFiles(scanImageFiles(previewsDir), PREVIEW_EXTENSION_PRIORITY);

        List<String> sortedStems = new ArrayList<String>(originalFiles.keySet());
        Collections.sort(sortedStems);

        List<WallpaperResponse> wallpapers = new ArrayList<WallpaperResponse>();
        int index = 1;

        for (String stem : sortedStems) {
            Path originalPath = originalFiles.get(stem);
            Path previewPath = previewFiles.containsKey(stem) ? previewFiles.get(stem) : originalPath;
            String previewUrl = toPublicUrl(catalogRoot, previewPath);
            String fullUrl = toPublicUrl(catalogRoot, originalPath);

            wallpapers.add(new WallpaperResponse(
                definition.getSlug() + "-" + index,
                definition.getDisplayName() + "壁纸" + index,
                originalPath.getFileName().toString(),
                previewUrl,
                fullUrl,
                fullUrl
            ));
            index++;
        }

        String coverImageUrl = wallpapers.isEmpty() ? "" : wallpapers.get(0).getPreviewUrl();
        return new BrandCatalogResponse(
            definition.getSlug(),
            definition.getDisplayName(),
            definition.getFolderName(),
            wallpapers.size(),
            coverImageUrl,
            wallpapers
        );
    }

    private Map<String, Path> selectPreferredFiles(List<Path> files, List<String> extensionPriority) {
        Map<String, List<Path>> byStem = new LinkedHashMap<String, List<Path>>();
        for (Path file : files) {
            String stem = stripExtension(file.getFileName().toString());
            if (!byStem.containsKey(stem)) {
                byStem.put(stem, new ArrayList<Path>());
            }
            byStem.get(stem).add(file);
        }

        Map<String, Path> result = new LinkedHashMap<String, Path>();
        for (Map.Entry<String, List<Path>> entry : byStem.entrySet()) {
            List<Path> candidates = entry.getValue();
            candidates.sort(new Comparator<Path>() {
                @Override
                public int compare(Path left, Path right) {
                    return Integer.compare(
                        extensionPriority.indexOf(extensionOf(left)),
                        extensionPriority.indexOf(extensionOf(right))
                    );
                }
            });
            result.put(entry.getKey(), candidates.get(0));
        }

        return result;
    }

    private List<Path> scanImageFiles(Path directory) {
        if (!Files.isDirectory(directory)) {
            return Collections.emptyList();
        }

        try (Stream<Path> files = Files.list(directory)) {
            return files
                .filter(Files::isRegularFile)
                .filter(this::isSupportedImageFile)
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        } catch (IOException exception) {
            throw new IllegalStateException("读取壁纸目录失败：" + directory, exception);
        }
    }

    private boolean isSupportedImageFile(Path path) {
        String extension = extensionOf(path);
        return ORIGINAL_EXTENSION_PRIORITY.contains(extension);
    }

    private String extensionOf(Path path) {
        return extensionOf(path.getFileName().toString());
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? fileName : fileName.substring(0, dotIndex);
    }

    private String toPublicUrl(Path catalogRoot, Path file) {
        String relativePath = catalogRoot.relativize(file).toString().replace("\\", "/");
        return "/cars/" + relativePath;
    }
}
