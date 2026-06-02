package com.vehiclewallpaper.backend.admin;

import javax.validation.constraints.NotBlank;

public class AdminAuthLoginRequest {

    @NotBlank(message = "请输入管理员账号。")
    private String username;

    @NotBlank(message = "请输入管理员密码。")
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
