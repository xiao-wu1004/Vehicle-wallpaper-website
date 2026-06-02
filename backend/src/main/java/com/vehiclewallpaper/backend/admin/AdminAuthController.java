package com.vehiclewallpaper.backend.admin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthenticationService adminAuthenticationService;

    public AdminAuthController(AdminAuthenticationService adminAuthenticationService) {
        this.adminAuthenticationService = adminAuthenticationService;
    }

    @GetMapping("/options")
    public AdminAuthOptionsResponse options() {
        return adminAuthenticationService.getOptions();
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AdminAuthLoginResponse login(@Valid @RequestBody AdminAuthLoginRequest request) {
        return adminAuthenticationService.login(request);
    }
}
