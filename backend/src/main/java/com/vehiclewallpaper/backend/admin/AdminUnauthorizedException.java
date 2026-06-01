package com.vehiclewallpaper.backend.admin;

public class AdminUnauthorizedException extends RuntimeException {

    public AdminUnauthorizedException(String message) {
        super(message);
    }
}
