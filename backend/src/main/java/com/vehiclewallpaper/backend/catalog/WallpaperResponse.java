package com.vehiclewallpaper.backend.catalog;

public class WallpaperResponse {

    private final String id;
    private final String title;
    private final String fileName;
    private final String previewUrl;
    private final String fullUrl;
    private final String downloadUrl;

    public WallpaperResponse(String id, String title, String fileName, String previewUrl, String fullUrl, String downloadUrl) {
        this.id = id;
        this.title = title;
        this.fileName = fileName;
        this.previewUrl = previewUrl;
        this.fullUrl = fullUrl;
        this.downloadUrl = downloadUrl;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
