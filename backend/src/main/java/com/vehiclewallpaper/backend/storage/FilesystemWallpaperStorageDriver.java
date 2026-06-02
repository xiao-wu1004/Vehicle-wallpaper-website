package com.vehiclewallpaper.backend.storage;

import com.vehiclewallpaper.backend.config.CatalogProperties;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FilesystemWallpaperStorageDriver implements WallpaperStorageDriver {

    public static final String PROVIDER_ID = "filesystem";

    private final CatalogProperties catalogProperties;

    public FilesystemWallpaperStorageDriver(CatalogProperties catalogProperties) {
        this.catalogProperties = catalogProperties;
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public StoredAsset store(String storageKey, MultipartFile file) throws IOException {
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target);
        }
        return new StoredAsset(getProviderId(), storageKey, toPublicUrl(storageKey));
    }

    @Override
    public StoredAsset move(String sourceKey, String targetKey) throws IOException {
        Path source = resolve(sourceKey);
        Path target = resolve(targetKey);
        if (!Files.exists(source)) {
            return new StoredAsset(getProviderId(), targetKey, toPublicUrl(targetKey));
        }

        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (FileAlreadyExistsException exception) {
            throw exception;
        } catch (IOException atomicMoveFailure) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return new StoredAsset(getProviderId(), targetKey, toPublicUrl(targetKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    @Override
    public String toPublicUrl(String storageKey) {
        String normalized = normalizeStorageKey(storageKey);
        String[] segments = normalized.split("/");
        return "/cars/" + Arrays.stream(segments)
            .map(this::encodePathSegment)
            .collect(Collectors.joining("/"));
    }

    private Path resolve(String storageKey) {
        Path root = Paths.get(catalogProperties.getRootPath()).toAbsolutePath().normalize();
        Path resolved = root.resolve(normalizeStorageKey(storageKey)).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Resolved storage path is outside the catalog root.");
        }
        return resolved;
    }

    private String normalizeStorageKey(String storageKey) {
        String normalized = storageKey == null ? "" : storageKey.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("..")) {
            throw new IllegalArgumentException("Storage key cannot contain path traversal characters.");
        }
        return normalized;
    }

    private String encodePathSegment(String rawSegment) {
        try {
            return URLEncoder.encode(rawSegment, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("Unable to encode storage path segment.", exception);
        }
    }
}
