package com.vehiclewallpaper.backend.catalog;

public class BrandDefinition {

    private final String slug;
    private final String displayName;
    private final String folderName;

    public BrandDefinition(String slug, String displayName, String folderName) {
        this.slug = slug;
        this.displayName = displayName;
        this.folderName = folderName;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFolderName() {
        return folderName;
    }
}
