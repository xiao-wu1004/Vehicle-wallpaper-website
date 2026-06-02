package com.vehiclewallpaper.backend.admin;

import javax.validation.constraints.NotBlank;

public class AdminAuthLoginRequest {

    @NotBlank(message = "Please enter the admin username.")
    private String username;

    @NotBlank(message = "Please enter the admin password.")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
