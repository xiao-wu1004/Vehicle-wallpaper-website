package com.vehiclewallpaper.backend.admin;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class AdminBrandUpsertRequest {

    @NotBlank(message = "Brand slug is required.")
    @Size(max = 64, message = "Brand slug must be 64 characters or fewer.")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Brand slug may contain lowercase letters, numbers, and hyphens only.")
    private String slug;

    @NotBlank(message = "Brand display name is required.")
    @Size(max = 128, message = "Brand display name must be 128 characters or fewer.")
    private String displayName;

    @NotBlank(message = "Brand folder name is required.")
    @Size(max = 128, message = "Brand folder name must be 128 characters or fewer.")
    private String folderName;

    @Min(value = 1, message = "Brand sort order must be at least 1.")
    private Integer sortOrder;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
