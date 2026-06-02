package com.vehiclewallpaper.backend.admin;

import com.vehiclewallpaper.backend.catalog.BrandEntity;
import com.vehiclewallpaper.backend.catalog.BrandRepository;
import com.vehiclewallpaper.backend.catalog.CatalogOverviewResponse;
import com.vehiclewallpaper.backend.catalog.CatalogService;
import com.vehiclewallpaper.backend.catalog.WallpaperDownloadEventRepository;
import com.vehiclewallpaper.backend.catalog.WallpaperEntity;
import com.vehiclewallpaper.backend.catalog.WallpaperFavoriteRepository;
import com.vehiclewallpaper.backend.catalog.WallpaperRepository;
import com.vehiclewallpaper.backend.config.CatalogProperties;
import com.vehiclewallpaper.backend.feedback.FeedbackMessage;
import com.vehiclewallpaper.backend.feedback.FeedbackRepository;
import com.vehiclewallpaper.backend.feedback.FeedbackStatus;
import com.vehiclewallpaper.backend.storage.StoredAsset;
import com.vehiclewallpaper.backend.storage.WallpaperStorageManager;
import com.vehiclewallpaper.backend.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminService {

    private static final List<String> SUPPORTED_IMAGE_EXTENSIONS = java.util.Arrays.asList("jpg", "jpeg", "png", "webp");

    private final CatalogService catalogService;
    private final CatalogProperties catalogProperties;
    private final BrandRepository brandRepository;
    private final WallpaperRepository wallpaperRepository;
    private final WallpaperFavoriteRepository wallpaperFavoriteRepository;
    private final WallpaperDownloadEventRepository wallpaperDownloadEventRepository;
    private final FeedbackRepository feedbackRepository;
    private final AdminOperationLogService adminOperationLogService;
    private final WallpaperStorageManager wallpaperStorageManager;

    public AdminService(CatalogService catalogService,
                        CatalogProperties catalogProperties,
                        BrandRepository brandRepository,
                        WallpaperRepository wallpaperRepository,
                        WallpaperFavoriteRepository wallpaperFavoriteRepository,
                        WallpaperDownloadEventRepository wallpaperDownloadEventRepository,
                        FeedbackRepository feedbackRepository,
                        AdminOperationLogService adminOperationLogService,
                        WallpaperStorageManager wallpaperStorageManager) {
        this.catalogService = catalogService;
        this.catalogProperties = catalogProperties;
        this.brandRepository = brandRepository;
        this.wallpaperRepository = wallpaperRepository;
        this.wallpaperFavoriteRepository = wallpaperFavoriteRepository;
        this.wallpaperDownloadEventRepository = wallpaperDownloadEventRepository;
        this.feedbackRepository = feedbackRepository;
        this.adminOperationLogService = adminOperationLogService;
        this.wallpaperStorageManager = wallpaperStorageManager;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        CatalogOverviewResponse overview = catalogService.getOverview();
        List<WallpaperEntity> wallpapers = wallpaperRepository.findAllForAdmin();

        long activeWallpapers = 0;
        for (WallpaperEntity wallpaper : wallpapers) {
            if (wallpaper.isActive()) {
                activeWallpapers++;
            }
        }

        return new AdminDashboardResponse(
            overview.getGeneratedAt(),
            brandRepository.count(),
            wallpapers.size(),
            activeWallpapers,
            wallpapers.size() - activeWallpapers,
            feedbackRepository.countByStatus(FeedbackStatus.PENDING),
            feedbackRepository.countByStatus(FeedbackStatus.APPROVED),
            feedbackRepository.countByStatus(FeedbackStatus.REJECTED),
            feedbackRepository.countByFeaturedTrue()
        );
    }

    @Transactional
    public CatalogOverviewResponse refreshCatalog() {
        CatalogOverviewResponse overview = catalogService.refreshCatalog();
        adminOperationLogService.logCurrentAction(
            "CATALOG_REFRESH",
            "CATALOG",
            "public-gallery",
            "Ran a manual catalog sync from the admin console."
        );
        return overview;
    }

    @Transactional(readOnly = true)
    public List<AdminBrandResponse> getBrands() {
        List<AdminBrandResponse> responses = new ArrayList<AdminBrandResponse>();
        for (BrandEntity brand : brandRepository.findAllByOrderBySortOrderAsc()) {
            responses.add(toBrandResponse(brand));
        }
        return responses;
    }

    @Transactional
    public AdminBrandResponse createBrand(AdminBrandUpsertRequest request) {
        String slug = normalizeBrandSlug(request.getSlug());
        String displayName = normalizeBrandDisplayName(request.getDisplayName());
        String folderName = normalizeFolderName(request.getFolderName());

        if (brandRepository.findBySlugIgnoreCase(slug).isPresent()) {
            throw new IllegalArgumentException("Brand slug already exists: " + slug);
        }

        if (brandRepository.findByFolderNameIgnoreCase(folderName).isPresent()) {
            throw new IllegalArgumentException("Brand folder already exists: " + folderName);
        }

        ensureBrandDirectories(folderName);

        BrandEntity brand = new BrandEntity();
        brand.setSlug(slug);
        brand.setDisplayName(displayName);
        brand.setFolderName(folderName);
        brand.setSortOrder(request.getSortOrder() == null ? nextBrandSortOrder() : request.getSortOrder().intValue());

        brand = brandRepository.save(brand);
        catalogService.invalidateOverview();
        adminOperationLogService.logCurrentAction(
            "BRAND_CREATE",
            "BRAND",
            brand.getId().toString(),
            "Created brand \"" + brand.getDisplayName() + "\" with slug \"" + brand.getSlug() + "\"."
        );
        return toBrandResponse(brand);
    }

    @Transactional
    public AdminBrandResponse updateBrand(Long brandId, AdminBrandUpsertRequest request) {
        BrandEntity brand = brandRepository.findById(brandId)
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + brandId));

        String slug = normalizeBrandSlug(request.getSlug());
        String displayName = normalizeBrandDisplayName(request.getDisplayName());
        String folderName = normalizeFolderName(request.getFolderName());

        assertBrandSlugAvailable(slug, brandId);
        assertBrandFolderAvailable(folderName, brandId);

        String oldFolderName = brand.getFolderName();
        if (!oldFolderName.equals(folderName)) {
            renameBrandDirectories(oldFolderName, folderName);
        }

        brand.setSlug(slug);
        brand.setDisplayName(displayName);
        brand.setFolderName(folderName);
        if (request.getSortOrder() != null) {
            brand.setSortOrder(request.getSortOrder().intValue());
        }

        brand = brandRepository.save(brand);

        for (WallpaperEntity wallpaper : wallpaperRepository.findByBrandIdOrderBySortOrderAsc(brand.getId())) {
            realignWallpaperStorageAfterBrandRename(wallpaper, oldFolderName, folderName);
            wallpaperRepository.save(wallpaper);
        }

        catalogService.invalidateOverview();
        adminOperationLogService.logCurrentAction(
            "BRAND_UPDATE",
            "BRAND",
            brand.getId().toString(),
            "Updated brand \"" + brand.getDisplayName() + "\" and folder \"" + brand.getFolderName() + "\"."
        );
        return toBrandResponse(brand);
    }

    @Transactional
    public void deleteBrand(Long brandId) {
        BrandEntity brand = brandRepository.findById(brandId)
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + brandId));
        int wallpaperCount = wallpaperRepository.findByBrandIdOrderBySortOrderAsc(brand.getId()).size();

        for (WallpaperEntity wallpaper : wallpaperRepository.findByBrandIdOrderBySortOrderAsc(brand.getId())) {
            deleteWallpaperFiles(wallpaper);
        }

        wallpaperFavoriteRepository.deleteByBrandId(brand.getId());
        wallpaperDownloadEventRepository.deleteByBrandId(brand.getId());
        wallpaperRepository.deleteByBrandId(brand.getId());
        brandRepository.delete(brand);

        deleteDirectoryIfExists(resolveBrandDirectory(brand.getFolderName()));
        deleteDirectoryIfExists(resolveBrandPreviewDirectory(brand.getFolderName()));
        catalogService.invalidateOverview();
        adminOperationLogService.logCurrentAction(
            "BRAND_DELETE",
            "BRAND",
            brandId.toString(),
            "Deleted brand \"" + brand.getDisplayName() + "\" and removed " + wallpaperCount + " wallpaper record(s)."
        );
    }

    @Transactional(readOnly = true)
    public List<AdminWallpaperResponse> getWallpapers(String brandSlug, Boolean active) {
        List<AdminWallpaperResponse> responses = new ArrayList<AdminWallpaperResponse>();

        for (WallpaperEntity wallpaper : wallpaperRepository.findAllForAdmin()) {
            boolean brandMatches = brandSlug == null || brandSlug.trim().isEmpty()
                || wallpaper.getBrand().getSlug().equalsIgnoreCase(brandSlug.trim());
            boolean activeMatches = active == null || wallpaper.isActive() == active.booleanValue();

            if (brandMatches && activeMatches) {
                responses.add(toWallpaperResponse(wallpaper));
            }
        }

        return responses;
    }

    @Transactional
    public AdminWallpaperResponse uploadWallpaper(Long brandId,
                                                  String title,
                                                  Integer sortOrder,
                                                  Boolean active,
                                                  MultipartFile file,
                                                  MultipartFile previewFile) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Wallpaper image file is required.");
        }

        BrandEntity brand = brandRepository.findById(brandId)
            .orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + brandId));

        String sanitizedOriginalFileName = sanitizeFileName(file.getOriginalFilename());
        String originalExtension = extensionOf(sanitizedOriginalFileName);
        requireSupportedImageExtension(originalExtension, "wallpaper");

        String baseName = stripExtension(sanitizedOriginalFileName);
        String storedBaseName = nextAvailableBaseName(brand.getFolderName(), baseName, originalExtension);
        String storedOriginalFileName = storedBaseName + "." + originalExtension;
        String originalStorageKey = buildOriginalStorageKey(brand.getFolderName(), storedOriginalFileName);

        String previewExtension = "";
        if (previewFile != null && !previewFile.isEmpty()) {
            previewExtension = extensionOf(sanitizeFileName(previewFile.getOriginalFilename()));
            requireSupportedImageExtension(previewExtension, "preview");
        }

        try {
            ensureBrandDirectories(brand.getFolderName());
            StoredAsset originalAsset = wallpaperStorageManager.store(originalStorageKey, file);

            String previewStorageKey = originalStorageKey;
            String previewUrl;
            if (previewFile != null && !previewFile.isEmpty()) {
                String storedPreviewFileName = storedBaseName + "." + previewExtension;
                previewStorageKey = buildPreviewStorageKey(brand.getFolderName(), storedPreviewFileName);
                previewUrl = wallpaperStorageManager.store(previewStorageKey, previewFile).getPublicUrl();
            } else {
                previewUrl = originalAsset.getPublicUrl();
            }

            WallpaperEntity wallpaper = new WallpaperEntity();
            wallpaper.setBrand(brand);
            wallpaper.setSlug(nextWallpaperSlug(brand.getSlug()));
            wallpaper.setTitle(normalizeWallpaperTitle(title, brand, storedOriginalFileName, sortOrder));
            wallpaper.setFileName(storedOriginalFileName);
            wallpaper.setStorageProvider(originalAsset.getProvider());
            wallpaper.setStorageKey(originalStorageKey);
            wallpaper.setPreviewStorageKey(previewStorageKey);
            wallpaper.setPreviewUrl(previewUrl);
            wallpaper.setFullUrl(originalAsset.getPublicUrl());
            wallpaper.setDownloadUrl(originalAsset.getPublicUrl());
            wallpaper.setSortOrder(sortOrder == null ? nextWallpaperSortOrder(brand.getId()) : sortOrder.intValue());
            wallpaper.setActive(active == null || active.booleanValue());

            wallpaper = wallpaperRepository.save(wallpaper);
            catalogService.invalidateOverview();
            adminOperationLogService.logCurrentAction(
                "WALLPAPER_UPLOAD",
                "WALLPAPER",
                wallpaper.getId().toString(),
                "Uploaded wallpaper \"" + wallpaper.getTitle() + "\" for brand \"" + brand.getDisplayName() + "\"."
            );
            return toWallpaperResponse(wallpaper);
        } catch (IOException exception) {
            deleteStorageSilently(wallpaperStorageManager.getDefaultProvider(), originalStorageKey);
            if (previewFile != null && !previewFile.isEmpty()) {
                deleteStorageSilently(
                    wallpaperStorageManager.getDefaultProvider(),
                    buildPreviewStorageKey(brand.getFolderName(), storedBaseName + "." + previewExtension)
                );
            }
            throw new IllegalStateException("Failed to store wallpaper files.", exception);
        }
    }

    @Transactional
    public void deleteWallpaper(Long wallpaperId) {
        WallpaperEntity wallpaper = wallpaperRepository.findWithBrandById(wallpaperId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallpaper not found: " + wallpaperId));
        String wallpaperTitle = wallpaper.getTitle();
        String brandName = wallpaper.getBrand().getDisplayName();

        deleteWallpaperFiles(wallpaper);
        wallpaperFavoriteRepository.deleteByWallpaperId(wallpaper.getId());
        wallpaperDownloadEventRepository.deleteByWallpaperId(wallpaper.getId());
        wallpaperRepository.delete(wallpaper);
        catalogService.invalidateOverview();
        adminOperationLogService.logCurrentAction(
            "WALLPAPER_DELETE",
            "WALLPAPER",
            wallpaperId.toString(),
            "Deleted wallpaper \"" + wallpaperTitle + "\" from brand \"" + brandName + "\"."
        );
    }

    @Transactional
    public AdminWallpaperResponse updateWallpaper(Long wallpaperId, AdminWallpaperUpdateRequest request) {
        if (request.getTitle() == null && request.getSortOrder() == null && request.getActive() == null) {
            throw new IllegalArgumentException("At least one wallpaper field must be provided.");
        }

        WallpaperEntity wallpaper = wallpaperRepository.findWithBrandById(wallpaperId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallpaper not found: " + wallpaperId));

        if (request.getTitle() != null) {
            String normalizedTitle = request.getTitle().trim();
            if (normalizedTitle.isEmpty()) {
                throw new IllegalArgumentException("Wallpaper title cannot be blank.");
            }
            wallpaper.setTitle(normalizedTitle);
        }

        if (request.getSortOrder() != null) {
            wallpaper.setSortOrder(request.getSortOrder().intValue());
        }

        if (request.getActive() != null) {
            wallpaper.setActive(request.getActive().booleanValue());
        }

        wallpaper = wallpaperRepository.save(wallpaper);
        catalogService.invalidateOverview();
        adminOperationLogService.logCurrentAction(
            "WALLPAPER_UPDATE",
            "WALLPAPER",
            wallpaper.getId().toString(),
            "Updated wallpaper \"" + wallpaper.getTitle() + "\"."
        );
        return toWallpaperResponse(wallpaper);
    }

    @Transactional(readOnly = true)
    public List<AdminFeedbackResponse> getFeedback(String status, Boolean featured, int limit) {
        FeedbackStatus statusFilter = parseFeedbackStatus(status, true);
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<AdminFeedbackResponse> responses = new ArrayList<AdminFeedbackResponse>();

        for (FeedbackMessage message : feedbackRepository.findAllNewestFirst()) {
            boolean statusMatches = statusFilter == null || message.getStatus() == statusFilter;
            boolean featuredMatches = featured == null || message.isFeatured() == featured.booleanValue();

            if (statusMatches && featuredMatches) {
                responses.add(toFeedbackResponse(message));
            }

            if (responses.size() >= safeLimit) {
                break;
            }
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<AdminOperationLogResponse> getOperationLogs(int limit) {
        return adminOperationLogService.getRecentLogs(limit);
    }

    @Transactional
    public AdminFeedbackResponse updateFeedback(Long feedbackId, AdminFeedbackUpdateRequest request) {
        if (request.getStatus() == null && request.getFeatured() == null) {
            throw new IllegalArgumentException("At least one feedback field must be provided.");
        }

        FeedbackMessage message = feedbackRepository.findById(feedbackId)
            .orElseThrow(() -> new ResourceNotFoundException("Feedback not found: " + feedbackId));

        if (request.getStatus() != null) {
            message.setStatus(parseFeedbackStatus(request.getStatus(), false));
        }

        if (request.getFeatured() != null) {
            message.setFeatured(request.getFeatured().booleanValue());
        }

        FeedbackMessage updatedMessage = feedbackRepository.save(message);
        adminOperationLogService.logCurrentAction(
            "FEEDBACK_UPDATE",
            "FEEDBACK",
            updatedMessage.getId().toString(),
            "Updated feedback review to status " + updatedMessage.getStatus().name()
                + " and featured=" + updatedMessage.isFeatured() + "."
        );
        return toFeedbackResponse(updatedMessage);
    }

    private AdminBrandResponse toBrandResponse(BrandEntity brand) {
        return new AdminBrandResponse(
            brand.getId(),
            brand.getSlug(),
            brand.getDisplayName(),
            brand.getFolderName(),
            brand.getSortOrder(),
            wallpaperRepository.findByBrandIdOrderBySortOrderAsc(brand.getId()).size(),
            brand.getCreatedAt(),
            brand.getUpdatedAt()
        );
    }

    private AdminWallpaperResponse toWallpaperResponse(WallpaperEntity wallpaper) {
        return new AdminWallpaperResponse(
            wallpaper.getId(),
            wallpaper.getSlug(),
            wallpaper.getBrand().getSlug(),
            wallpaper.getBrand().getDisplayName(),
            wallpaper.getTitle(),
            wallpaper.getFileName(),
            wallpaper.getPreviewUrl(),
            wallpaper.getFullUrl(),
            wallpaper.getDownloadUrl(),
            wallpaper.getSortOrder(),
            wallpaper.isActive(),
            wallpaper.getCreatedAt(),
            wallpaper.getUpdatedAt()
        );
    }

    private AdminFeedbackResponse toFeedbackResponse(FeedbackMessage message) {
        return new AdminFeedbackResponse(
            message.getId(),
            message.getName(),
            message.getEmail(),
            message.getMessage(),
            message.getStatus().name(),
            message.isFeatured(),
            message.getSourcePage(),
            message.getUserAgent(),
            message.getCreatedAt()
        );
    }

    private FeedbackStatus parseFeedbackStatus(String rawStatus, boolean allowNull) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            if (allowNull) {
                return null;
            }
            throw new IllegalArgumentException("Feedback status cannot be blank.");
        }

        try {
            return FeedbackStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported feedback status: " + rawStatus);
        }
    }

    private void assertBrandSlugAvailable(String slug, Long currentBrandId) {
        BrandEntity existing = brandRepository.findBySlugIgnoreCase(slug).orElse(null);
        if (existing != null && !existing.getId().equals(currentBrandId)) {
            throw new IllegalArgumentException("Brand slug already exists: " + slug);
        }
    }

    private void assertBrandFolderAvailable(String folderName, Long currentBrandId) {
        BrandEntity existing = brandRepository.findByFolderNameIgnoreCase(folderName).orElse(null);
        if (existing != null && !existing.getId().equals(currentBrandId)) {
            throw new IllegalArgumentException("Brand folder already exists: " + folderName);
        }
    }

    private int nextBrandSortOrder() {
        int max = 0;
        for (BrandEntity brand : brandRepository.findAllByOrderBySortOrderAsc()) {
            max = Math.max(max, brand.getSortOrder());
        }
        return Math.max(1, max + 1);
    }

    private int nextWallpaperSortOrder(Long brandId) {
        int max = 0;
        for (WallpaperEntity wallpaper : wallpaperRepository.findByBrandIdOrderBySortOrderAsc(brandId)) {
            max = Math.max(max, wallpaper.getSortOrder());
        }
        return Math.max(1, max + 1);
    }

    private String nextWallpaperSlug(String brandSlug) {
        int nextNumber = 1;
        for (WallpaperEntity wallpaper : wallpaperRepository.findAllForAdmin()) {
            String prefix = brandSlug + "-";
            if (wallpaper.getSlug() != null && wallpaper.getSlug().startsWith(prefix)) {
                String suffix = wallpaper.getSlug().substring(prefix.length());
                try {
                    nextNumber = Math.max(nextNumber, Integer.parseInt(suffix) + 1);
                } catch (NumberFormatException ignored) {
                    nextNumber = Math.max(nextNumber, nextNumber + 1);
                }
            }
        }

        String candidate = brandSlug + "-" + nextNumber;
        while (wallpaperRepository.existsBySlugIgnoreCase(candidate)) {
            nextNumber++;
            candidate = brandSlug + "-" + nextNumber;
        }
        return candidate;
    }

    private String normalizeBrandSlug(String slug) {
        return normalizeRequired(slug, "Brand slug is required.").toLowerCase(Locale.ROOT);
    }

    private String normalizeBrandDisplayName(String displayName) {
        return normalizeRequired(displayName, "Brand display name is required.");
    }

    private String normalizeFolderName(String folderName) {
        String normalized = normalizeRequired(folderName, "Brand folder name is required.");
        if ("_thumb".equalsIgnoreCase(normalized)) {
            throw new IllegalArgumentException("Brand folder name cannot be _thumb.");
        }
        if (normalized.contains("..") || normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException("Brand folder name cannot contain path traversal characters.");
        }
        return normalized;
    }

    private String normalizeWallpaperTitle(String title, BrandEntity brand, String storedOriginalFileName, Integer sortOrder) {
        String normalizedTitle = title == null ? "" : title.trim();
        if (!normalizedTitle.isEmpty()) {
            return normalizedTitle;
        }

        int fallbackSortOrder = sortOrder == null ? nextWallpaperSortOrder(brand.getId()) : sortOrder.intValue();
        String baseName = stripExtension(storedOriginalFileName).replace('-', ' ').replace('_', ' ').trim();
        return baseName.isEmpty() ? brand.getDisplayName() + "壁纸" + fallbackSortOrder : baseName;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String sanitizeFileName(String originalFileName) {
        String sanitized = Paths.get(originalFileName == null ? "" : originalFileName).getFileName().toString().trim();
        if (sanitized.isEmpty() || !sanitized.contains(".")) {
            throw new IllegalArgumentException("Uploaded file must have a valid file name.");
        }
        return sanitized;
    }

    private void requireSupportedImageExtension(String extension, String label) {
        if (!SUPPORTED_IMAGE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported " + label + " file type: " + extension);
        }
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? "" : fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? fileName : fileName.substring(0, dotIndex);
    }

    private String nextAvailableBaseName(String folderName, String baseName, String extension) {
        String normalizedBaseName = baseName == null || baseName.trim().isEmpty() ? "wallpaper" : baseName.trim();
        List<WallpaperEntity> existingWallpapers = wallpaperRepository.findAllForAdmin();
        int suffix = 0;
        while (true) {
            String candidate = suffix == 0 ? normalizedBaseName : normalizedBaseName + "-" + suffix;
            String expectedFileName = candidate + "." + extension;
            if (!fileNameExistsForBrand(existingWallpapers, folderName, expectedFileName)) {
                return candidate;
            }
            suffix++;
        }
    }

    private void ensureBrandDirectories(String folderName) {
        try {
            Files.createDirectories(resolveBrandDirectory(folderName));
            Files.createDirectories(resolveBrandPreviewDirectory(folderName));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare brand directories.", exception);
        }
    }

    private void renameBrandDirectories(String oldFolderName, String newFolderName) {
        Path oldOriginals = resolveBrandDirectory(oldFolderName);
        Path newOriginals = resolveBrandDirectory(newFolderName);
        Path oldPreviews = resolveBrandPreviewDirectory(oldFolderName);
        Path newPreviews = resolveBrandPreviewDirectory(newFolderName);

        try {
            if (Files.exists(newOriginals) && !Files.isSameFile(oldOriginals, newOriginals)) {
                throw new IllegalArgumentException("Target brand folder already exists: " + newFolderName);
            }
        } catch (IOException ignored) {
            // If the source folder does not exist yet we can continue and just create the target.
        }

        moveDirectoryIfExists(oldOriginals, newOriginals);
        moveDirectoryIfExists(oldPreviews, newPreviews);
        ensureBrandDirectories(newFolderName);
    }

    private void moveDirectoryIfExists(Path source, Path target) {
        if (source == null || !Files.exists(source)) {
            return;
        }

        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (FileAlreadyExistsException exception) {
            throw new IllegalArgumentException("Target folder already exists: " + target.getFileName(), exception);
        } catch (IOException atomicMoveFailure) {
            try {
                Files.move(source, target);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to rename brand folders.", exception);
            }
        }
    }

    private void deleteWallpaperFiles(WallpaperEntity wallpaper) {
        String provider = normalizedStorageProvider(wallpaper);
        String originalKey = normalizeStorageKey(
            wallpaper.getStorageKey(),
            buildOriginalStorageKey(wallpaper.getBrand().getFolderName(), wallpaper.getFileName())
        );
        deleteFileIfExists(provider, originalKey);

        String previewFileName = extractFileNameFromPublicUrl(wallpaper.getPreviewUrl());
        String previewKey = normalizeStorageKey(
            wallpaper.getPreviewStorageKey(),
            previewFileName.isEmpty() || previewFileName.equals(wallpaper.getFileName())
                ? originalKey
                : buildPreviewStorageKey(wallpaper.getBrand().getFolderName(), previewFileName)
        );
        if (!previewKey.equals(originalKey)) {
            deleteFileIfExists(provider, previewKey);
        }
    }

    private void deleteFileIfExists(String provider, String storageKey) {
        if (storageKey == null || storageKey.trim().isEmpty()) {
            return;
        }

        try {
            wallpaperStorageManager.delete(provider, storageKey);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete file: " + storageKey, exception);
        }
    }

    private void deleteDirectoryIfExists(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }

        List<Path> pathsToDelete;
        try {
            pathsToDelete = Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .collect(java.util.stream.Collectors.toList());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect directory for deletion: " + directory, exception);
        }

        List<String> failedPaths = new ArrayList<>();
        for (Path path : pathsToDelete) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException exception) {
                failedPaths.add(path.toString());
            }
        }

        if (!failedPaths.isEmpty()) {
            throw new IllegalStateException(
                "Failed to delete " + failedPaths.size() + " item(s) in " + directory
                + " (e.g. " + failedPaths.get(0) + ")");
        }
    }

    private Path catalogRootPath() {
        return Paths.get(catalogProperties.getRootPath()).toAbsolutePath().normalize();
    }

    private Path resolveBrandDirectory(String folderName) {
        Path root = catalogRootPath();
        Path resolved = root.resolve(folderName).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Resolved brand directory is outside the catalog root.");
        }
        return resolved;
    }

    private Path resolveBrandPreviewDirectory(String folderName) {
        Path previewRoot = catalogRootPath().resolve("_thumb").normalize();
        Path resolved = previewRoot.resolve(folderName).normalize();
        if (!resolved.startsWith(previewRoot)) {
            throw new IllegalArgumentException("Resolved preview directory is outside the preview root.");
        }
        return resolved;
    }

    private String buildBrandOriginalUrl(String folderName, String fileName) {
        return "/cars/" + encodePathSegment(folderName) + "/" + encodePathSegment(fileName);
    }

    private String buildBrandPreviewUrl(String folderName, String fileName) {
        return "/cars/_thumb/" + encodePathSegment(folderName) + "/" + encodePathSegment(fileName);
    }

    private String encodePathSegment(String rawSegment) {
        try {
            return URLEncoder.encode(rawSegment, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("Unable to encode file path segment.", exception);
        }
    }

    private boolean fileNameExistsForBrand(List<WallpaperEntity> wallpapers, String folderName, String fileName) {
        for (WallpaperEntity wallpaper : wallpapers) {
            if (wallpaper.getBrand() != null
                && folderName.equalsIgnoreCase(wallpaper.getBrand().getFolderName())
                && fileName.equalsIgnoreCase(wallpaper.getFileName())) {
                return true;
            }
        }
        return false;
    }

    private void realignWallpaperStorageAfterBrandRename(WallpaperEntity wallpaper, String oldFolderName, String newFolderName) {
        String storageProvider = normalizedStorageProvider(wallpaper);
        String oldOriginalKey = normalizeStorageKey(
            wallpaper.getStorageKey(),
            buildOriginalStorageKey(oldFolderName, wallpaper.getFileName())
        );
        String newOriginalKey = buildOriginalStorageKey(newFolderName, wallpaper.getFileName());

        String previewFileName = extractFileNameFromPublicUrl(wallpaper.getPreviewUrl());
        if (previewFileName.isEmpty()) {
            previewFileName = wallpaper.getFileName();
        }

        String oldPreviewKey = normalizeStorageKey(
            wallpaper.getPreviewStorageKey(),
            previewFileName.equals(wallpaper.getFileName())
                ? oldOriginalKey
                : buildPreviewStorageKey(oldFolderName, previewFileName)
        );
        String newPreviewKey = previewFileName.equals(wallpaper.getFileName())
            ? newOriginalKey
            : buildPreviewStorageKey(newFolderName, previewFileName);

        try {
            StoredAsset originalAsset = wallpaperStorageManager.move(storageProvider, oldOriginalKey, newOriginalKey);
            wallpaper.setStorageProvider(originalAsset.getProvider());
            wallpaper.setStorageKey(newOriginalKey);
            wallpaper.setFullUrl(originalAsset.getPublicUrl());
            wallpaper.setDownloadUrl(originalAsset.getPublicUrl());

            if (oldPreviewKey.equals(oldOriginalKey)) {
                wallpaper.setPreviewStorageKey(newOriginalKey);
                wallpaper.setPreviewUrl(originalAsset.getPublicUrl());
            } else {
                StoredAsset previewAsset = wallpaperStorageManager.move(storageProvider, oldPreviewKey, newPreviewKey);
                wallpaper.setPreviewStorageKey(newPreviewKey);
                wallpaper.setPreviewUrl(previewAsset.getPublicUrl());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to rename wallpaper storage assets.", exception);
        }
    }

    private String buildOriginalStorageKey(String folderName, String fileName) {
        return folderName + "/" + fileName;
    }

    private String buildPreviewStorageKey(String folderName, String fileName) {
        return "_thumb/" + folderName + "/" + fileName;
    }

    private String normalizedStorageProvider(WallpaperEntity wallpaper) {
        String provider = wallpaper.getStorageProvider();
        return provider == null || provider.trim().isEmpty()
            ? "filesystem"
            : provider.trim();
    }

    private String normalizeStorageKey(String existingStorageKey, String fallbackStorageKey) {
        String candidate = existingStorageKey == null || existingStorageKey.trim().isEmpty()
            ? fallbackStorageKey
            : existingStorageKey;
        return candidate.replace("\\", "/");
    }

    private void deleteStorageSilently(String provider, String storageKey) {
        try {
            wallpaperStorageManager.delete(provider, storageKey);
        } catch (IOException ignored) {
            // Best-effort cleanup for partially uploaded files.
        }
    }

    private String extractFileNameFromPublicUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.trim().isEmpty()) {
            return "";
        }

        int lastSlash = publicUrl.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == publicUrl.length() - 1) {
            return "";
        }

        String encodedFileName = publicUrl.substring(lastSlash + 1);
        try {
            return URLDecoder.decode(encodedFileName, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("Unable to decode file path segment.", exception);
        }
    }
}
