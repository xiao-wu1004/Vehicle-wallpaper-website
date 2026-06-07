package com.vehiclewallpaper.backend.catalog;

import com.vehiclewallpaper.backend.config.CatalogProperties;
import com.vehiclewallpaper.backend.storage.FilesystemWallpaperStorageDriver;
import com.vehiclewallpaper.backend.web.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CatalogService {

    private static final int PROFILE_DOWNLOAD_BATCH_SIZE = 24;

    private static final List<BrandDefinition> DEFAULT_BRAND_DEFINITIONS = Arrays.asList(
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
    private final BrandRepository brandRepository;
    private final WallpaperRepository wallpaperRepository;
    private final WallpaperFavoriteRepository wallpaperFavoriteRepository;
    private final WallpaperDownloadEventRepository wallpaperDownloadEventRepository;
    private final WallpaperMetricRepository wallpaperMetricRepository;
    private final TransactionTemplate transactionTemplate;

    private volatile CatalogOverviewResponse cachedNeutralOverview;
    private volatile CatalogOverviewResponse cachedGlobalOverview;

    public CatalogService(CatalogProperties catalogProperties,
                          BrandRepository brandRepository,
                          WallpaperRepository wallpaperRepository,
                          WallpaperFavoriteRepository wallpaperFavoriteRepository,
                          WallpaperDownloadEventRepository wallpaperDownloadEventRepository,
                          WallpaperMetricRepository wallpaperMetricRepository,
                          PlatformTransactionManager transactionManager) {
        this.catalogProperties = catalogProperties;
        this.brandRepository = brandRepository;
        this.wallpaperRepository = wallpaperRepository;
        this.wallpaperFavoriteRepository = wallpaperFavoriteRepository;
        this.wallpaperDownloadEventRepository = wallpaperDownloadEventRepository;
        this.wallpaperMetricRepository = wallpaperMetricRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @PostConstruct
    public void warmUp() {
        if (catalogProperties.isSyncOnStartup()) {
            refreshCatalog();
        }
    }

    public CatalogOverviewResponse getOverview() {
        return getOverview(null);
    }

    @Transactional(readOnly = true)
    public CatalogSummaryResponse getSummary(String visitorKey) {
        CatalogOverviewResponse overview = getOverview(visitorKey);
        List<CatalogSummaryBrandResponse> brands = new ArrayList<CatalogSummaryBrandResponse>();
        for (BrandCatalogResponse brand : overview.getBrands()) {
            brands.add(new CatalogSummaryBrandResponse(
                brand.getSlug(),
                brand.getDisplayName(),
                brand.getWallpaperCount(),
                brand.getCoverImageUrl()
            ));
        }

        return new CatalogSummaryResponse(
            overview.getGeneratedAt(),
            overview.getTotalBrands(),
            overview.getTotalWallpapers(),
            overview.getTotalFavorites(),
            overview.getTotalDownloads(),
            brands,
            overview.getTrendingWallpapers()
        );
    }

    @Transactional(readOnly = true)
    public CatalogOverviewResponse getOverview(String visitorKey) {
        CatalogOverviewResponse globalOverview = getGlobalOverview();
        String normalizedVisitorKey = normalizeValue(visitorKey);
        if (normalizedVisitorKey.isEmpty()) {
            return globalOverview;
        }

        Set<Long> favoriteWallpaperIds = new LinkedHashSet<Long>(
            wallpaperFavoriteRepository.findFavoriteWallpaperIdsByVisitorKey(normalizedVisitorKey)
        );
        if (favoriteWallpaperIds.isEmpty()) {
            return globalOverview;
        }

        return applyVisitorFavorites(globalOverview, favoriteWallpaperIds);
    }

    public void invalidateOverview() {
        cachedNeutralOverview = null;
        cachedGlobalOverview = null;
    }

    public List<BrandCatalogResponse> getBrands() {
        return getBrands(null);
    }

    @Transactional(readOnly = true)
    public List<BrandCatalogResponse> getBrands(String visitorKey) {
        return getOverview(visitorKey).getBrands();
    }

    public BrandCatalogResponse getBrand(String brandSlug) {
        return getBrand(brandSlug, null);
    }

    @Transactional(readOnly = true)
    public BrandCatalogResponse getBrand(String brandSlug, String visitorKey) {
        for (BrandCatalogResponse brand : getBrands(visitorKey)) {
            if (brand.getSlug().equalsIgnoreCase(brandSlug)) {
                return brand;
            }
        }
        throw new ResourceNotFoundException("未找到品牌：" + brandSlug);
    }

    @Transactional(readOnly = true)
    public List<WallpaperResponse> search(String brandSlug,
                                          String query,
                                          String sort,
                                          int limit,
                                          boolean favoritesOnly,
                                          String visitorKey) {
        List<WallpaperResponse> candidates = new ArrayList<WallpaperResponse>();
        String normalizedBrand = normalizeValue(brandSlug);
        String normalizedQuery = normalizeValue(query).toLowerCase(Locale.ROOT);

        if (!normalizedBrand.isEmpty()) {
            candidates.addAll(getBrand(normalizedBrand, visitorKey).getWallpapers());
        } else {
            for (BrandCatalogResponse brand : getBrands(visitorKey)) {
                candidates.addAll(brand.getWallpapers());
            }
        }

        List<WallpaperResponse> filtered = new ArrayList<WallpaperResponse>();
        for (WallpaperResponse wallpaper : candidates) {
            boolean queryMatches = normalizedQuery.isEmpty()
                || containsIgnoreCase(wallpaper.getTitle(), normalizedQuery)
                || containsIgnoreCase(wallpaper.getFileName(), normalizedQuery)
                || containsIgnoreCase(wallpaper.getId(), normalizedQuery)
                || containsIgnoreCase(wallpaper.getBrandSlug(), normalizedQuery);

            boolean favoritesMatch = !favoritesOnly || wallpaper.isFavorited();
            if (queryMatches && favoritesMatch) {
                filtered.add(wallpaper);
            }
        }

        sortWallpapers(filtered, sort);

        if (filtered.size() <= limit) {
            return filtered;
        }
        return new ArrayList<WallpaperResponse>(filtered.subList(0, limit));
    }

    @Transactional(readOnly = true)
    public CatalogProfileResponse getProfile(String visitorKey, int favoriteLimit, int downloadLimit) {
        return getProfile(visitorKey, favoriteLimit, downloadLimit, false, "", "");
    }

    @Transactional(readOnly = true)
    public CatalogProfileResponse getProfile(String visitorKey,
                                             int favoriteLimit,
                                             int downloadLimit,
                                             boolean authenticated,
                                             String displayName,
                                             String email) {
        String normalizedVisitorKey = requireVisitorKey(visitorKey);
        long favoriteCount = wallpaperFavoriteRepository.countByVisitorKey(normalizedVisitorKey);
        long downloadCount = wallpaperDownloadEventRepository.countByVisitorKey(normalizedVisitorKey);

        List<WallpaperFavoriteEntity> favoriteEntities = favoriteLimit < 1
            ? Collections.<WallpaperFavoriteEntity>emptyList()
            : wallpaperFavoriteRepository.findRecentByVisitorKeyWithWallpaper(
                normalizedVisitorKey,
                PageRequest.of(0, favoriteLimit)
            );
        List<WallpaperDownloadEventEntity> downloadEntities = collectRecentDownloadEntities(normalizedVisitorKey, downloadLimit);

        List<Long> profileWallpaperIds = collectProfileWallpaperIds(favoriteEntities, downloadEntities);
        Map<Long, WallpaperMetricEntity> metricsByWallpaperId = profileWallpaperIds.isEmpty()
            ? Collections.<Long, WallpaperMetricEntity>emptyMap()
            : indexWallpaperMetrics(wallpaperMetricRepository.findAllByWallpaperIdIn(profileWallpaperIds));

        Set<Long> favoritedWallpaperIds = new LinkedHashSet<Long>();
        for (WallpaperFavoriteEntity favoriteEntity : favoriteEntities) {
            favoritedWallpaperIds.add(favoriteEntity.getWallpaper().getId());
        }
        if (!downloadEntities.isEmpty()) {
            favoritedWallpaperIds.addAll(
                wallpaperFavoriteRepository.findFavoriteWallpaperIdsByVisitorKeyAndWallpaperIds(normalizedVisitorKey, profileWallpaperIds)
            );
        }

        List<WallpaperResponse> favorites = new ArrayList<WallpaperResponse>();
        for (WallpaperFavoriteEntity favoriteEntity : favoriteEntities) {
            favorites.add(toProfileWallpaperResponse(
                favoriteEntity.getWallpaper(),
                metricsByWallpaperId,
                true
            ));
        }

        List<WallpaperResponse> recentDownloads = new ArrayList<WallpaperResponse>();
        for (WallpaperDownloadEventEntity downloadEntity : downloadEntities) {
            WallpaperEntity wallpaper = downloadEntity.getWallpaper();
            recentDownloads.add(toProfileWallpaperResponse(
                wallpaper,
                metricsByWallpaperId,
                favoritedWallpaperIds.contains(wallpaper.getId())
            ));
        }

        return new CatalogProfileResponse(
            normalizedVisitorKey,
            authenticated,
            normalizeValue(displayName),
            normalizeValue(email),
            favoriteCount,
            downloadCount,
            favorites,
            recentDownloads
        );
    }

    @Transactional
    public WallpaperInteractionResponse addFavorite(String wallpaperSlug, String visitorKey) {
        WallpaperEntity wallpaper = requireWallpaperBySlug(wallpaperSlug);
        String normalizedVisitorKey = requireVisitorKey(visitorKey);

        if (!wallpaperFavoriteRepository.existsByVisitorKeyAndWallpaperId(normalizedVisitorKey, wallpaper.getId())) {
            WallpaperFavoriteEntity favorite = new WallpaperFavoriteEntity();
            favorite.setWallpaper(wallpaper);
            favorite.setVisitorKey(normalizedVisitorKey);
            wallpaperFavoriteRepository.save(favorite);
            adjustFavoriteMetric(wallpaper, 1L);
        }

        invalidateMetricsOverview();
        return buildInteractionResponse(wallpaper, true);
    }

    @Transactional
    public WallpaperInteractionResponse removeFavorite(String wallpaperSlug, String visitorKey) {
        WallpaperEntity wallpaper = requireWallpaperBySlug(wallpaperSlug);
        String normalizedVisitorKey = requireVisitorKey(visitorKey);

        if (wallpaperFavoriteRepository.deleteByVisitorKeyAndWallpaperId(normalizedVisitorKey, wallpaper.getId()) > 0) {
            adjustFavoriteMetric(wallpaper, -1L);
        }

        invalidateMetricsOverview();
        return buildInteractionResponse(wallpaper, false);
    }

    @Transactional
    public WallpaperInteractionResponse recordDownload(String wallpaperSlug, String visitorKey) {
        WallpaperEntity wallpaper = requireWallpaperBySlug(wallpaperSlug);
        String normalizedVisitorKey = requireVisitorKey(visitorKey);

        WallpaperDownloadEventEntity downloadEvent = new WallpaperDownloadEventEntity();
        downloadEvent.setWallpaper(wallpaper);
        downloadEvent.setVisitorKey(normalizedVisitorKey);
        wallpaperDownloadEventRepository.save(downloadEvent);
        adjustDownloadMetric(wallpaper, 1L);

        invalidateMetricsOverview();
        boolean favorited = wallpaperFavoriteRepository.existsByVisitorKeyAndWallpaperId(normalizedVisitorKey, wallpaper.getId());
        return buildInteractionResponse(wallpaper, favorited);
    }

    public synchronized CatalogOverviewResponse refreshCatalog() {
        transactionTemplate.executeWithoutResult(status -> syncCatalogToDatabase());

        cachedNeutralOverview = buildNeutralOverview();
        CatalogOverviewResponse neutralOverview = cachedNeutralOverview;
        cachedGlobalOverview = decorateOverview(neutralOverview, createGlobalMetricsSnapshot(neutralOverview));
        return cachedGlobalOverview;
    }

    private CatalogOverviewResponse getNeutralOverview() {
        CatalogOverviewResponse overview = cachedNeutralOverview;
        return overview == null ? refreshNeutralOverview() : overview;
    }

    private CatalogOverviewResponse getGlobalOverview() {
        CatalogOverviewResponse overview = cachedGlobalOverview;
        return overview == null ? refreshGlobalOverview() : overview;
    }

    private synchronized CatalogOverviewResponse refreshNeutralOverview() {
        if (cachedNeutralOverview != null) {
            return cachedNeutralOverview;
        }

        if (catalogProperties.isSyncOnStartup()) {
            transactionTemplate.executeWithoutResult(status -> syncCatalogToDatabase());
        }

        cachedNeutralOverview = buildNeutralOverview();
        return cachedNeutralOverview;
    }

    private synchronized CatalogOverviewResponse refreshGlobalOverview() {
        if (cachedGlobalOverview != null) {
            return cachedGlobalOverview;
        }

        CatalogOverviewResponse neutralOverview = getNeutralOverview();
        cachedGlobalOverview = decorateOverview(neutralOverview, createGlobalMetricsSnapshot(neutralOverview));
        return cachedGlobalOverview;
    }

    private CatalogOverviewResponse buildNeutralOverview() {
        List<BrandEntity> brandsInOrder = brandRepository.findAllByOrderBySortOrderAsc();
        Map<Long, List<WallpaperResponse>> wallpapersByBrandId = new LinkedHashMap<Long, List<WallpaperResponse>>();
        for (WallpaperEntity entity : wallpaperRepository.findAllActiveWithBrandOrder()) {
            Long brandId = entity.getBrand().getId();
            if (!wallpapersByBrandId.containsKey(brandId)) {
                wallpapersByBrandId.put(brandId, new ArrayList<WallpaperResponse>());
            }
            wallpapersByBrandId.get(brandId).add(toNeutralWallpaperResponse(entity));
        }

        List<BrandCatalogResponse> brands = new ArrayList<BrandCatalogResponse>();
        int totalWallpapers = 0;

        for (BrandEntity brand : brandsInOrder) {
            List<WallpaperResponse> wallpapers = wallpapersByBrandId.containsKey(brand.getId())
                ? wallpapersByBrandId.get(brand.getId())
                : Collections.<WallpaperResponse>emptyList();
            totalWallpapers += wallpapers.size();
            String coverImageUrl = wallpapers.isEmpty() ? "" : wallpapers.get(0).getPreviewUrl();
            brands.add(new BrandCatalogResponse(
                brand.getSlug(),
                brand.getDisplayName(),
                brand.getDisplayName(),
                brand.getFolderName(),
                wallpapers.size(),
                coverImageUrl,
                wallpapers
            ));
        }

        return new CatalogOverviewResponse(
            LocalDateTime.now(),
            brands.size(),
            totalWallpapers,
            0L,
            0L,
            Collections.<WallpaperResponse>emptyList(),
            brands
        );
    }

    private CatalogOverviewResponse decorateOverview(CatalogOverviewResponse neutralOverview, CatalogMetricsSnapshot metricsSnapshot) {
        List<BrandCatalogResponse> brands = new ArrayList<BrandCatalogResponse>();
        List<WallpaperResponse> flattened = new ArrayList<WallpaperResponse>();

        for (BrandCatalogResponse brand : neutralOverview.getBrands()) {
            List<WallpaperResponse> wallpapers = new ArrayList<WallpaperResponse>();
            for (WallpaperResponse wallpaper : brand.getWallpapers()) {
                WallpaperResponse decorated = decorateWallpaper(wallpaper, metricsSnapshot);
                wallpapers.add(decorated);
                flattened.add(decorated);
            }

            String coverImageUrl = wallpapers.isEmpty() ? "" : wallpapers.get(0).getPreviewUrl();
            brands.add(new BrandCatalogResponse(
                brand.getSlug(),
                brand.getDisplayName(),
                brand.getDisplayName(),
                brand.getFolderName(),
                wallpapers.size(),
                coverImageUrl,
                wallpapers
            ));
        }

        sortWallpapers(flattened, "hot");
        List<WallpaperResponse> trending = flattened.size() > 8
            ? new ArrayList<WallpaperResponse>(flattened.subList(0, 8))
            : flattened;

        return new CatalogOverviewResponse(
            neutralOverview.getGeneratedAt(),
            neutralOverview.getTotalBrands(),
            neutralOverview.getTotalWallpapers(),
            metricsSnapshot.getTotalFavorites(),
            metricsSnapshot.getTotalDownloads(),
            trending,
            brands
        );
    }

    private CatalogOverviewResponse applyVisitorFavorites(CatalogOverviewResponse globalOverview, Set<Long> favoriteWallpaperIds) {
        if (favoriteWallpaperIds == null || favoriteWallpaperIds.isEmpty()) {
            return globalOverview;
        }

        Map<Long, WallpaperResponse> overrides = new LinkedHashMap<Long, WallpaperResponse>();
        boolean changed = false;
        List<BrandCatalogResponse> brands = new ArrayList<BrandCatalogResponse>();
        for (BrandCatalogResponse brand : globalOverview.getBrands()) {
            boolean brandChanged = false;
            List<WallpaperResponse> wallpapers = new ArrayList<WallpaperResponse>();
            for (WallpaperResponse wallpaper : brand.getWallpapers()) {
                WallpaperResponse updatedWallpaper = withFavoritedState(wallpaper, favoriteWallpaperIds, overrides);
                brandChanged = brandChanged || updatedWallpaper != wallpaper;
                wallpapers.add(updatedWallpaper);
            }

            if (brandChanged) {
                changed = true;
                brands.add(new BrandCatalogResponse(
                    brand.getSlug(),
                    brand.getName(),
                    brand.getDisplayName(),
                    brand.getFolderName(),
                    brand.getWallpaperCount(),
                    brand.getCoverImageUrl(),
                    wallpapers
                ));
            } else {
                brands.add(brand);
            }
        }

        List<WallpaperResponse> trending = new ArrayList<WallpaperResponse>();
        for (WallpaperResponse wallpaper : globalOverview.getTrendingWallpapers()) {
            WallpaperResponse updatedWallpaper = withFavoritedState(wallpaper, favoriteWallpaperIds, overrides);
            changed = changed || updatedWallpaper != wallpaper;
            trending.add(updatedWallpaper);
        }

        if (!changed) {
            return globalOverview;
        }

        return new CatalogOverviewResponse(
            globalOverview.getGeneratedAt(),
            globalOverview.getTotalBrands(),
            globalOverview.getTotalWallpapers(),
            globalOverview.getTotalFavorites(),
            globalOverview.getTotalDownloads(),
            trending,
            brands
        );
    }

    private WallpaperResponse toNeutralWallpaperResponse(WallpaperEntity entity) {
        return new WallpaperResponse(
            entity.getSlug(),
            entity.getId(),
            entity.getBrand().getSlug(),
            entity.getTitle(),
            entity.getFileName(),
            entity.getPreviewUrl(),
            entity.getFullUrl(),
            entity.getDownloadUrl(),
            entity.getSortOrder(),
            0L,
            0L,
            0.0d,
            false,
            entity.getCreatedAt()
        );
    }

    private WallpaperResponse decorateWallpaper(WallpaperResponse wallpaper, CatalogMetricsSnapshot metricsSnapshot) {
        long favoriteCount = metricsSnapshot.favoriteCountFor(wallpaper.getWallpaperId());
        long downloadCount = metricsSnapshot.downloadCountFor(wallpaper.getWallpaperId());
        return new WallpaperResponse(
            wallpaper.getId(),
            wallpaper.getWallpaperId(),
            wallpaper.getBrandSlug(),
            wallpaper.getTitle(),
            wallpaper.getFileName(),
            wallpaper.getPreviewUrl(),
            wallpaper.getFullUrl(),
            wallpaper.getDownloadUrl(),
            wallpaper.getSortOrder(),
            favoriteCount,
            downloadCount,
            calculateHotScore(favoriteCount, downloadCount),
            metricsSnapshot.isFavorited(wallpaper.getWallpaperId()),
            wallpaper.getCreatedAt()
        );
    }

    private WallpaperResponse toProfileWallpaperResponse(WallpaperEntity wallpaper,
                                                         Map<Long, WallpaperMetricEntity> metricsByWallpaperId,
                                                         boolean favorited) {
        WallpaperMetricEntity metric = metricsByWallpaperId.get(wallpaper.getId());
        long favoriteCount = metric == null ? 0L : metric.getFavoriteCount();
        long downloadCount = metric == null ? 0L : metric.getDownloadCount();
        return new WallpaperResponse(
            wallpaper.getSlug(),
            wallpaper.getId(),
            wallpaper.getBrand().getSlug(),
            wallpaper.getTitle(),
            wallpaper.getFileName(),
            wallpaper.getPreviewUrl(),
            wallpaper.getFullUrl(),
            wallpaper.getDownloadUrl(),
            wallpaper.getSortOrder(),
            favoriteCount,
            downloadCount,
            calculateHotScore(favoriteCount, downloadCount),
            favorited,
            wallpaper.getCreatedAt()
        );
    }

    private CatalogMetricsSnapshot createGlobalMetricsSnapshot(CatalogOverviewResponse overview) {
        List<Long> wallpaperIds = new ArrayList<Long>();
        for (BrandCatalogResponse brand : overview.getBrands()) {
            for (WallpaperResponse wallpaper : brand.getWallpapers()) {
                wallpaperIds.add(wallpaper.getWallpaperId());
            }
        }

        if (wallpaperIds.isEmpty()) {
            return new CatalogMetricsSnapshot(0L, 0L, Collections.<Long, Long>emptyMap(), Collections.<Long, Long>emptyMap(), Collections.<Long>emptySet());
        }

        Map<Long, Long> favoriteCounts = new LinkedHashMap<Long, Long>();
        Map<Long, Long> downloadCounts = new LinkedHashMap<Long, Long>();
        long totalFavorites = 0L;
        long totalDownloads = 0L;

        for (WallpaperMetricEntity metric : wallpaperMetricRepository.findAllByWallpaperIdIn(wallpaperIds)) {
            favoriteCounts.put(metric.getWallpaperId(), metric.getFavoriteCount());
            downloadCounts.put(metric.getWallpaperId(), metric.getDownloadCount());
            totalFavorites += metric.getFavoriteCount();
            totalDownloads += metric.getDownloadCount();
        }

        return new CatalogMetricsSnapshot(
            totalFavorites,
            totalDownloads,
            favoriteCounts,
            downloadCounts,
            Collections.<Long>emptySet()
        );
    }

    private void sortWallpapers(List<WallpaperResponse> wallpapers, String sort) {
        final String normalizedSort = normalizeValue(sort).toLowerCase(Locale.ROOT);
        Comparator<WallpaperResponse> comparator;

        if ("latest".equals(normalizedSort) || "newest".equals(normalizedSort)) {
            comparator = Comparator.comparing(WallpaperResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(WallpaperResponse::getSortOrder)
                .thenComparing(WallpaperResponse::getId);
        } else if ("title".equals(normalizedSort) || "name".equals(normalizedSort)) {
            comparator = Comparator.comparing(WallpaperResponse::getTitle, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(WallpaperResponse::getId);
        } else if ("downloads".equals(normalizedSort)) {
            comparator = Comparator.comparingLong(WallpaperResponse::getDownloadCount).reversed()
                .thenComparing(Comparator.comparingLong(WallpaperResponse::getFavoriteCount).reversed())
                .thenComparing(Comparator.comparing(WallpaperResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        } else if ("favorites".equals(normalizedSort)) {
            comparator = Comparator.comparingLong(WallpaperResponse::getFavoriteCount).reversed()
                .thenComparing(Comparator.comparingLong(WallpaperResponse::getDownloadCount).reversed())
                .thenComparing(Comparator.comparing(WallpaperResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        } else {
            comparator = Comparator.comparingDouble(WallpaperResponse::getHotScore).reversed()
                .thenComparing(Comparator.comparingLong(WallpaperResponse::getDownloadCount).reversed())
                .thenComparing(Comparator.comparingLong(WallpaperResponse::getFavoriteCount).reversed())
                .thenComparing(Comparator.comparing(WallpaperResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        wallpapers.sort(comparator);
    }

    private List<WallpaperDownloadEventEntity> collectRecentDownloadEntities(String visitorKey, int downloadLimit) {
        if (downloadLimit < 1) {
            return Collections.emptyList();
        }

        List<WallpaperDownloadEventEntity> recentDownloads = new ArrayList<WallpaperDownloadEventEntity>();
        Set<Long> seenWallpaperIds = new LinkedHashSet<Long>();
        int page = 0;

        while (recentDownloads.size() < downloadLimit) {
            List<WallpaperDownloadEventEntity> batch = wallpaperDownloadEventRepository.findRecentByVisitorKeyWithWallpaper(
                visitorKey,
                PageRequest.of(page, Math.max(downloadLimit, PROFILE_DOWNLOAD_BATCH_SIZE))
            );
            if (batch.isEmpty()) {
                break;
            }

            for (WallpaperDownloadEventEntity downloadEvent : batch) {
                WallpaperEntity wallpaper = downloadEvent.getWallpaper();
                if (wallpaper == null || !seenWallpaperIds.add(wallpaper.getId())) {
                    continue;
                }

                recentDownloads.add(downloadEvent);
                if (recentDownloads.size() >= downloadLimit) {
                    break;
                }
            }

            if (batch.size() < Math.max(downloadLimit, PROFILE_DOWNLOAD_BATCH_SIZE)) {
                break;
            }
            page++;
        }

        return recentDownloads;
    }

    private List<Long> collectProfileWallpaperIds(List<WallpaperFavoriteEntity> favorites,
                                                  List<WallpaperDownloadEventEntity> downloads) {
        Set<Long> wallpaperIds = new LinkedHashSet<Long>();
        for (WallpaperFavoriteEntity favorite : favorites) {
            if (favorite.getWallpaper() != null) {
                wallpaperIds.add(favorite.getWallpaper().getId());
            }
        }
        for (WallpaperDownloadEventEntity download : downloads) {
            if (download.getWallpaper() != null) {
                wallpaperIds.add(download.getWallpaper().getId());
            }
        }
        return new ArrayList<Long>(wallpaperIds);
    }

    private Map<String, WallpaperResponse> flattenWallpapersBySlug(CatalogOverviewResponse overview) {
        Map<String, WallpaperResponse> bySlug = new LinkedHashMap<String, WallpaperResponse>();
        for (BrandCatalogResponse brand : overview.getBrands()) {
            for (WallpaperResponse wallpaper : brand.getWallpapers()) {
                bySlug.put(wallpaper.getId(), wallpaper);
            }
        }
        return bySlug;
    }

    private WallpaperInteractionResponse buildInteractionResponse(WallpaperEntity wallpaper,
                                                                  boolean favorited) {
        WallpaperMetricEntity metric = ensureWallpaperMetric(wallpaper);
        long favoriteCount = metric.getFavoriteCount();
        long downloadCount = metric.getDownloadCount();

        return new WallpaperInteractionResponse(
            wallpaper.getSlug(),
            favorited,
            favoriteCount,
            downloadCount,
            calculateHotScore(favoriteCount, downloadCount)
        );
    }

    private WallpaperResponse withFavoritedState(WallpaperResponse wallpaper,
                                                 Set<Long> favoriteWallpaperIds,
                                                 Map<Long, WallpaperResponse> overrides) {
        boolean shouldBeFavorited = favoriteWallpaperIds.contains(wallpaper.getWallpaperId());
        if (wallpaper.isFavorited() == shouldBeFavorited) {
            return wallpaper;
        }

        WallpaperResponse existingOverride = overrides.get(wallpaper.getWallpaperId());
        if (existingOverride != null) {
            return existingOverride;
        }

        WallpaperResponse overridden = new WallpaperResponse(
            wallpaper.getId(),
            wallpaper.getWallpaperId(),
            wallpaper.getBrandSlug(),
            wallpaper.getTitle(),
            wallpaper.getFileName(),
            wallpaper.getPreviewUrl(),
            wallpaper.getFullUrl(),
            wallpaper.getDownloadUrl(),
            wallpaper.getSortOrder(),
            wallpaper.getFavoriteCount(),
            wallpaper.getDownloadCount(),
            wallpaper.getHotScore(),
            shouldBeFavorited,
            wallpaper.getCreatedAt()
        );
        overrides.put(wallpaper.getWallpaperId(), overridden);
        return overridden;
    }

    private double calculateHotScore(long favoriteCount, long downloadCount) {
        return favoriteCount * 4.0d + downloadCount * 1.5d;
    }

    private WallpaperEntity requireWallpaperBySlug(String wallpaperSlug) {
        return wallpaperRepository.findWithBrandBySlug(normalizeValue(wallpaperSlug))
            .orElseThrow(() -> new ResourceNotFoundException("未找到壁纸：" + wallpaperSlug));
    }

    private String requireVisitorKey(String visitorKey) {
        String normalized = normalizeValue(visitorKey);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Visitor key is required for this action.");
        }
        if (!normalized.matches("[A-Za-z0-9_-]{16,96}")) {
            throw new IllegalArgumentException("Visitor key format is invalid.");
        }
        return normalized;
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private Map<Long, WallpaperMetricEntity> indexWallpaperMetrics(List<WallpaperMetricEntity> metrics) {
        Map<Long, WallpaperMetricEntity> metricsByWallpaperId = new LinkedHashMap<Long, WallpaperMetricEntity>();
        for (WallpaperMetricEntity metric : metrics) {
            metricsByWallpaperId.put(metric.getWallpaperId(), metric);
        }
        return metricsByWallpaperId;
    }

    private WallpaperMetricEntity ensureWallpaperMetric(WallpaperEntity wallpaper) {
        return wallpaperMetricRepository.findByWallpaperId(wallpaper.getId())
            .orElseGet(() -> {
                WallpaperMetricEntity metric = new WallpaperMetricEntity();
                metric.setWallpaper(wallpaper);
                metric.setFavoriteCount(0L);
                metric.setDownloadCount(0L);
                return wallpaperMetricRepository.save(metric);
            });
    }

    private void adjustFavoriteMetric(WallpaperEntity wallpaper, long delta) {
        ensureWallpaperMetric(wallpaper);
        wallpaperMetricRepository.adjustFavoriteCount(wallpaper.getId(), delta, LocalDateTime.now());
    }

    private void adjustDownloadMetric(WallpaperEntity wallpaper, long delta) {
        ensureWallpaperMetric(wallpaper);
        wallpaperMetricRepository.adjustDownloadCount(wallpaper.getId(), delta, LocalDateTime.now());
    }

    private void invalidateMetricsOverview() {
        cachedGlobalOverview = null;
    }

    private void syncCatalogToDatabase() {
        Path catalogRoot = Paths.get(catalogProperties.getRootPath()).toAbsolutePath().normalize();
        ensureDefaultBrands();

        for (BrandEntity brand : brandRepository.findAllByOrderBySortOrderAsc()) {
            syncWallpapersForBrand(catalogRoot, brand);
        }
    }

    private void ensureDefaultBrands() {
        if (brandRepository.count() > 0) {
            return;
        }

        for (int index = 0; index < DEFAULT_BRAND_DEFINITIONS.size(); index++) {
            BrandDefinition definition = DEFAULT_BRAND_DEFINITIONS.get(index);
            BrandEntity brand = new BrandEntity();
            brand.setSlug(definition.getSlug());
            brand.setDisplayName(definition.getDisplayName());
            brand.setFolderName(definition.getFolderName());
            brand.setSortOrder(index + 1);
            brandRepository.save(brand);
        }
    }

    private void syncWallpapersForBrand(Path catalogRoot, BrandEntity brand) {
        Path originalsDirectory = catalogRoot.resolve(brand.getFolderName());
        Path previewsDirectory = catalogRoot.resolve("_thumb").resolve(brand.getFolderName());

        Map<String, Path> originalFiles = selectPreferredFiles(scanImageFiles(originalsDirectory), ORIGINAL_EXTENSION_PRIORITY);
        Map<String, Path> previewFiles = selectPreferredFiles(scanImageFiles(previewsDirectory), PREVIEW_EXTENSION_PRIORITY);

        List<String> sortedStems = new ArrayList<String>(originalFiles.keySet());
        Collections.sort(sortedStems);

        List<WallpaperEntity> existingWallpapers = wallpaperRepository.findByBrandIdOrderBySortOrderAsc(brand.getId());
        Map<String, WallpaperEntity> existingFilesystemWallpapers = new LinkedHashMap<String, WallpaperEntity>();
        int nextSlugNumber = determineNextWallpaperSlugNumber(brand.getSlug(), existingWallpapers);
        int nextSortOrder = determineNextSortOrder(existingWallpapers);

        for (WallpaperEntity wallpaper : existingWallpapers) {
            if (isFilesystemWallpaper(wallpaper)) {
                existingFilesystemWallpapers.put(wallpaper.getFileName().toLowerCase(Locale.ROOT), wallpaper);
            }
        }

        for (String stem : sortedStems) {
            Path originalPath = originalFiles.get(stem);
            Path previewPath = previewFiles.containsKey(stem) ? previewFiles.get(stem) : originalPath;
            String fileName = originalPath.getFileName().toString();
            WallpaperEntity wallpaper = existingFilesystemWallpapers.remove(fileName.toLowerCase(Locale.ROOT));

            if (wallpaper == null) {
                wallpaper = new WallpaperEntity();
                wallpaper.setBrand(brand);
                wallpaper.setSlug(buildNextWallpaperSlug(brand.getSlug(), nextSlugNumber));
                wallpaper.setTitle(defaultWallpaperTitle(brand, nextSortOrder));
                wallpaper.setSortOrder(nextSortOrder);
                wallpaper.setActive(true);
                nextSlugNumber++;
                nextSortOrder++;
            }

            String storageKey = relativizeStorageKey(catalogRoot, originalPath);
            String previewStorageKey = relativizeStorageKey(catalogRoot, previewPath);

            wallpaper.setFileName(fileName);
            wallpaper.setStorageProvider(FilesystemWallpaperStorageDriver.PROVIDER_ID);
            wallpaper.setStorageKey(storageKey);
            wallpaper.setPreviewStorageKey(previewStorageKey);
            wallpaper.setPreviewUrl(toFilesystemPublicUrl(previewStorageKey));
            wallpaper.setFullUrl(toFilesystemPublicUrl(storageKey));
            wallpaper.setDownloadUrl(toFilesystemPublicUrl(storageKey));

            if (normalizeValue(wallpaper.getTitle()).isEmpty()) {
                wallpaper.setTitle(defaultWallpaperTitle(brand, wallpaper.getSortOrder()));
            }
            if (wallpaper.getSortOrder() < 1) {
                wallpaper.setSortOrder(nextSortOrder++);
            }

            wallpaper = wallpaperRepository.save(wallpaper);
            ensureWallpaperMetric(wallpaper);
        }

        for (WallpaperEntity removedWallpaper : existingFilesystemWallpapers.values()) {
            deleteUserEngagementForWallpaper(removedWallpaper.getId());
            wallpaperRepository.delete(removedWallpaper);
        }
    }

    private boolean isFilesystemWallpaper(WallpaperEntity wallpaper) {
        String provider = normalizeValue(wallpaper.getStorageProvider()).toLowerCase(Locale.ROOT);
        return provider.isEmpty() || FilesystemWallpaperStorageDriver.PROVIDER_ID.equals(provider);
    }

    private void deleteUserEngagementForWallpaper(Long wallpaperId) {
        wallpaperMetricRepository.deleteByWallpaperId(wallpaperId);
        wallpaperFavoriteRepository.deleteByWallpaperId(wallpaperId);
        wallpaperDownloadEventRepository.deleteByWallpaperId(wallpaperId);
    }

    private int determineNextWallpaperSlugNumber(String brandSlug, List<WallpaperEntity> existingWallpapers) {
        int max = 0;
        for (WallpaperEntity wallpaper : existingWallpapers) {
            String prefix = brandSlug + "-";
            if (wallpaper.getSlug() != null && wallpaper.getSlug().startsWith(prefix)) {
                String suffix = wallpaper.getSlug().substring(prefix.length());
                try {
                    max = Math.max(max, Integer.parseInt(suffix));
                } catch (NumberFormatException ignored) {
                    // Ignore custom legacy slugs.
                }
            }
        }
        return max + 1;
    }

    private int determineNextSortOrder(List<WallpaperEntity> existingWallpapers) {
        int max = 0;
        for (WallpaperEntity wallpaper : existingWallpapers) {
            max = Math.max(max, wallpaper.getSortOrder());
        }
        return Math.max(1, max + 1);
    }

    private String buildNextWallpaperSlug(String brandSlug, int nextNumber) {
        String candidate = brandSlug + "-" + nextNumber;
        int suffix = nextNumber;
        while (wallpaperRepository.existsBySlugIgnoreCase(candidate)) {
            suffix++;
            candidate = brandSlug + "-" + suffix;
        }
        return candidate;
    }

    private String defaultWallpaperTitle(BrandEntity brand, int sortOrder) {
        return brand.getDisplayName() + "壁纸" + sortOrder;
    }

    private Map<String, Path> selectPreferredFiles(List<Path> files, List<String> extensionPriority) {
        Map<String, List<Path>> grouped = new LinkedHashMap<String, List<Path>>();
        for (Path file : files) {
            String stem = stripExtension(file.getFileName().toString());
            if (!grouped.containsKey(stem)) {
                grouped.put(stem, new ArrayList<Path>());
            }
            grouped.get(stem).add(file);
        }

        Map<String, Path> selected = new LinkedHashMap<String, Path>();
        for (Map.Entry<String, List<Path>> entry : grouped.entrySet()) {
            List<Path> candidates = entry.getValue();
            candidates.sort(new Comparator<Path>() {
                @Override
                public int compare(Path left, Path right) {
                    return Integer.compare(priorityIndex(extensionPriority, extensionOf(left)), priorityIndex(extensionPriority, extensionOf(right)));
                }
            });
            selected.put(entry.getKey(), candidates.get(0));
        }

        return selected;
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
        return ORIGINAL_EXTENSION_PRIORITY.contains(extensionOf(path));
    }

    private int priorityIndex(List<String> extensionPriority, String extension) {
        int index = extensionPriority.indexOf(extension);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private String relativizeStorageKey(Path catalogRoot, Path file) {
        return catalogRoot.relativize(file).toString().replace("\\", "/");
    }

    private String toFilesystemPublicUrl(String storageKey) {
        String[] segments = storageKey.split("/");
        return "/cars/" + Arrays.stream(segments)
            .map(this::encodePathSegment)
            .collect(Collectors.joining("/"));
    }

    private String encodePathSegment(String rawSegment) {
        try {
            return URLEncoder.encode(rawSegment, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("无法编码资源路径：" + rawSegment, exception);
        }
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
}
