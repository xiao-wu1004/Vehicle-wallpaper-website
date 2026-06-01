package com.vehiclewallpaper.backend.admin;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public class AdminWallpaperUpdateRequest {

    @Size(max = 255, message = "Wallpaper title must be 255 characters or fewer.")
    private String title;

    @Min(value = 1, message = "sortOrder must be at least 1.")
    private Integer sortOrder;

    private Boolean active;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
