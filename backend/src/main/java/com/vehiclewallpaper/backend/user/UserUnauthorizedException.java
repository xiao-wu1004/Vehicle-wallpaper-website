package com.vehiclewallpaper.backend.user;

public class UserUnauthorizedException extends RuntimeException {

    public UserUnauthorizedException(String message) {
        super(message);
    }
}
