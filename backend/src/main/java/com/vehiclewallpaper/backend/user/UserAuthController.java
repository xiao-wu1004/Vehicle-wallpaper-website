package com.vehiclewallpaper.backend.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class UserAuthController {

    private final UserAuthenticationService userAuthenticationService;

    public UserAuthController(UserAuthenticationService userAuthenticationService) {
        this.userAuthenticationService = userAuthenticationService;
    }

    @GetMapping("/me")
    public UserAuthStatusResponse me(HttpServletRequest request) {
        return userAuthenticationService.getStatus(request);
    }

    @PostMapping("/register")
    public UserAuthSessionResponse register(@Valid @RequestBody UserAuthRegisterRequest request,
                                            HttpServletRequest servletRequest) {
        return userAuthenticationService.register(request, servletRequest);
    }

    @PostMapping("/login")
    public UserAuthSessionResponse login(@Valid @RequestBody UserAuthLoginRequest request,
                                         HttpServletRequest servletRequest) {
        return userAuthenticationService.login(request, servletRequest);
    }

    @PostMapping("/logout")
    public UserAuthStatusResponse logout(HttpServletRequest servletRequest) {
        return userAuthenticationService.logout(servletRequest);
    }
}
