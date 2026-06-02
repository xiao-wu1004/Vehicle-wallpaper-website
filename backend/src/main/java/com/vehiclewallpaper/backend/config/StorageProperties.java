package com.vehiclewallpaper.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String mode = "filesystem";
    private String s3Endpoint = "";
    private String s3Region = "ap-northeast-1";
    private String s3Bucket = "";
    private String s3AccessKey = "";
    private String s3SecretKey = "";
    private String s3KeyPrefix = "";
    private String s3PublicBaseUrl = "";
    private boolean s3PathStyleAccess;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getS3Endpoint() {
        return s3Endpoint;
    }

    public void setS3Endpoint(String s3Endpoint) {
        this.s3Endpoint = s3Endpoint;
    }

    public String getS3Region() {
        return s3Region;
    }

    public void setS3Region(String s3Region) {
        this.s3Region = s3Region;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public String getS3AccessKey() {
        return s3AccessKey;
    }

    public void setS3AccessKey(String s3AccessKey) {
        this.s3AccessKey = s3AccessKey;
    }

    public String getS3SecretKey() {
        return s3SecretKey;
    }

    public void setS3SecretKey(String s3SecretKey) {
        this.s3SecretKey = s3SecretKey;
    }

    public String getS3KeyPrefix() {
        return s3KeyPrefix;
    }

    public void setS3KeyPrefix(String s3KeyPrefix) {
        this.s3KeyPrefix = s3KeyPrefix;
    }

    public String getS3PublicBaseUrl() {
        return s3PublicBaseUrl;
    }

    public void setS3PublicBaseUrl(String s3PublicBaseUrl) {
        this.s3PublicBaseUrl = s3PublicBaseUrl;
    }

    public boolean isS3PathStyleAccess() {
        return s3PathStyleAccess;
    }

    public void setS3PathStyleAccess(boolean s3PathStyleAccess) {
        this.s3PathStyleAccess = s3PathStyleAccess;
    }
}
