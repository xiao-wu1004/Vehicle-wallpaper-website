package com.vehiclewallpaper.backend.storage;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.vehiclewallpaper.backend.config.StorageProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

public class S3WallpaperStorageDriver implements WallpaperStorageDriver {

    public static final String PROVIDER_ID = "s3";

    private final StorageProperties storageProperties;
    private final AmazonS3 amazonS3;

    public S3WallpaperStorageDriver(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.amazonS3 = buildClient(storageProperties);
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public StoredAsset store(String storageKey, MultipartFile file) throws IOException {
        String objectKey = buildObjectKey(storageKey);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(detectContentType(file));

        try (InputStream inputStream = file.getInputStream()) {
            amazonS3.putObject(storageProperties.getS3Bucket(), objectKey, inputStream, metadata);
        }
        return new StoredAsset(getProviderId(), storageKey, toPublicUrl(storageKey));
    }

    @Override
    public StoredAsset move(String sourceKey, String targetKey) {
        String sourceObjectKey = buildObjectKey(sourceKey);
        String targetObjectKey = buildObjectKey(targetKey);
        if (!amazonS3.doesObjectExist(storageProperties.getS3Bucket(), sourceObjectKey)) {
            return new StoredAsset(getProviderId(), targetKey, toPublicUrl(targetKey));
        }

        amazonS3.copyObject(new CopyObjectRequest(
            storageProperties.getS3Bucket(),
            sourceObjectKey,
            storageProperties.getS3Bucket(),
            targetObjectKey
        ));
        amazonS3.deleteObject(storageProperties.getS3Bucket(), sourceObjectKey);
        return new StoredAsset(getProviderId(), targetKey, toPublicUrl(targetKey));
    }

    @Override
    public void delete(String storageKey) {
        amazonS3.deleteObject(storageProperties.getS3Bucket(), buildObjectKey(storageKey));
    }

    @Override
    public String toPublicUrl(String storageKey) {
        String objectKey = buildObjectKey(storageKey);
        if (StringUtils.hasText(storageProperties.getS3PublicBaseUrl())) {
            return trimTrailingSlash(storageProperties.getS3PublicBaseUrl()) + "/"
                + encodeObjectKey(objectKey);
        }
        return amazonS3.getUrl(storageProperties.getS3Bucket(), objectKey).toString();
    }

    private AmazonS3 buildClient(StorageProperties properties) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
            .withPathStyleAccessEnabled(properties.isS3PathStyleAccess());

        if (StringUtils.hasText(properties.getS3Endpoint())) {
            builder.withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(properties.getS3Endpoint(), properties.getS3Region())
            );
        } else {
            builder.withRegion(properties.getS3Region());
        }

        if (StringUtils.hasText(properties.getS3AccessKey()) || StringUtils.hasText(properties.getS3SecretKey())) {
            builder.withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(properties.getS3AccessKey(), properties.getS3SecretKey())
            ));
        }

        return builder.build();
    }

    private String buildObjectKey(String storageKey) {
        String normalizedKey = normalizeStorageKey(storageKey);
        if (StringUtils.hasText(storageProperties.getS3KeyPrefix())) {
            return trimSlashes(storageProperties.getS3KeyPrefix()) + "/" + normalizedKey;
        }
        return normalizedKey;
    }

    private String detectContentType(MultipartFile file) {
        if (StringUtils.hasText(file.getContentType())) {
            return file.getContentType();
        }
        String lowerName = (file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase());
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
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

    private String encodeObjectKey(String objectKey) {
        return Arrays.stream(objectKey.split("/"))
            .map(this::encodePathSegment)
            .collect(Collectors.joining("/"));
    }

    private String encodePathSegment(String segment) {
        try {
            return URLEncoder.encode(segment, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("Unable to encode object storage key.", exception);
        }
    }

    private String trimTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String trimSlashes(String value) {
        String trimmed = trimTrailingSlash(value);
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }
}
