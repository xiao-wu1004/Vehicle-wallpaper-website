package com.vehiclewallpaper.backend.admin;

import javax.validation.constraints.NotBlank;

public class AdminResetUserPasswordRequest {

    @NotBlank(message = "New password is required.")
    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
