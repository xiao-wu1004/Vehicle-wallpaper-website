package com.vehiclewallpaper.backend.admin;

import com.vehiclewallpaper.backend.config.AdminSecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

@Service
public class AdminAuthenticationService {

    private static final String DEFAULT_LOGIN_USERNAME = "admin";
    private static final String AUTH_MODE_API_KEY = "API_KEY";
    private static final String AUTH_MODE_PASSWORD = "PASSWORD";

    private final AdminSecurityProperties adminSecurityProperties;
    private final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder base64UrlDecoder = Base64.getUrlDecoder();

    public AdminAuthenticationService(AdminSecurityProperties adminSecurityProperties) {
        this.adminSecurityProperties = adminSecurityProperties;
    }

    public AdminAuthOptionsResponse getOptions() {
        return new AdminAuthOptionsResponse(isLoginEnabled(), isApiKeyEnabled(), getLoginUsernameHint());
    }

    public AdminAuthLoginResponse login(AdminAuthLoginRequest request) {
        if (!isLoginEnabled()) {
            throw new AdminSecurityNotConfiguredException("管理员账号登录尚未配置。");
        }

        String incomingUsername = normalize(request.getUsername());
        String incomingPassword = normalize(request.getPassword());
        String configuredUsername = getEffectiveLoginUsername();
        String configuredPassword = getEffectiveLoginPassword();

        if (!constantEquals(configuredUsername, incomingUsername) || !constantEquals(configuredPassword, incomingPassword)) {
            throw new AdminUnauthorizedException("管理员账号或密码错误。");
        }

        long expiresAtEpochSecond = Instant.now().plusSeconds(getEffectiveTokenTtlHours() * 3600L).getEpochSecond();
        String accessToken = issueToken(configuredUsername, expiresAtEpochSecond);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(expiresAtEpochSecond), ZoneId.of("Asia/Shanghai"));
        return new AdminAuthLoginResponse(configuredUsername, AUTH_MODE_PASSWORD, accessToken, expiresAt);
    }

    public AdminIdentity authenticate(HttpServletRequest request) {
        String authorization = normalize(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authenticateBearerToken(authorization.substring(7).trim());
        }

        String configuredApiKey = normalize(adminSecurityProperties.getApiKey());
        String incomingApiKey = normalize(request.getHeader(adminSecurityProperties.getHeaderName()));
        if (!configuredApiKey.isEmpty() && constantEquals(configuredApiKey, incomingApiKey)) {
            return new AdminIdentity("api-key-admin", AUTH_MODE_API_KEY);
        }

        if (!isLoginEnabled() && configuredApiKey.isEmpty()) {
            throw new AdminSecurityNotConfiguredException("管理员登录或 API Key 尚未配置。");
        }

        if (!authorization.isEmpty()) {
            throw new AdminUnauthorizedException("管理员登录凭证无效或已过期。");
        }

        throw new AdminUnauthorizedException("缺少有效的管理员登录凭证。");
    }

    private AdminIdentity authenticateBearerToken(String rawToken) {
        if (!isLoginEnabled()) {
            throw new AdminSecurityNotConfiguredException("管理员账号登录尚未配置。");
        }

        if (rawToken.isEmpty()) {
            throw new AdminUnauthorizedException("缺少有效的管理员登录凭证。");
        }

        String[] parts = rawToken.split("\\.");
        if (parts.length != 3) {
            throw new AdminUnauthorizedException("管理员登录凭证无效或已过期。");
        }

        String payloadPart = parts[0];
        String expiresPart = parts[1];
        String signaturePart = parts[2];
        String signedData = payloadPart + "." + expiresPart;
        String expectedSignature = sign(signedData, getEffectiveTokenSecret());
        if (!constantEquals(expectedSignature, signaturePart)) {
            throw new AdminUnauthorizedException("管理员登录凭证无效或已过期。");
        }

        long expiresAtEpochSecond;
        try {
            expiresAtEpochSecond = Long.parseLong(expiresPart);
        } catch (NumberFormatException exception) {
            throw new AdminUnauthorizedException("管理员登录凭证无效或已过期。");
        }

        if (Instant.now().getEpochSecond() >= expiresAtEpochSecond) {
            throw new AdminUnauthorizedException("管理员登录凭证无效或已过期。");
        }

        String username;
        try {
            username = new String(base64UrlDecoder.decode(payloadPart), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new AdminUnauthorizedException("管理员登录凭证无效或已过期。");
        }

        if (!constantEquals(getEffectiveLoginUsername(), username)) {
            throw new AdminUnauthorizedException("管理员登录凭证无效或已过期。");
        }

        return new AdminIdentity(username, AUTH_MODE_PASSWORD);
    }

    private String issueToken(String username, long expiresAtEpochSecond) {
        String payloadPart = base64UrlEncoder.encodeToString(username.getBytes(StandardCharsets.UTF_8));
        String expiresPart = Long.toString(expiresAtEpochSecond);
        String signedData = payloadPart + "." + expiresPart;
        String signaturePart = sign(signedData, getEffectiveTokenSecret());
        return signedData + "." + signaturePart;
    }

    private String sign(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncoder.encodeToString(signature);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to sign admin token.", exception);
        }
    }

    private boolean isLoginEnabled() {
        return !getEffectiveLoginUsername().isEmpty() && !getEffectiveLoginPassword().isEmpty() && !getEffectiveTokenSecret().isEmpty();
    }

    private boolean isApiKeyEnabled() {
        return !normalize(adminSecurityProperties.getApiKey()).isEmpty();
    }

    private String getLoginUsernameHint() {
        return isLoginEnabled() ? getEffectiveLoginUsername() : "";
    }

    private String getEffectiveLoginUsername() {
        String configuredUsername = normalize(adminSecurityProperties.getLoginUsername());
        if (!configuredUsername.isEmpty()) {
            return configuredUsername;
        }

        return getEffectiveLoginPassword().isEmpty() ? "" : DEFAULT_LOGIN_USERNAME;
    }

    private String getEffectiveLoginPassword() {
        String configuredPassword = normalize(adminSecurityProperties.getLoginPassword());
        if (!configuredPassword.isEmpty()) {
            return configuredPassword;
        }

        return normalize(adminSecurityProperties.getApiKey());
    }

    private String getEffectiveTokenSecret() {
        String configuredTokenSecret = normalize(adminSecurityProperties.getTokenSecret());
        if (!configuredTokenSecret.isEmpty()) {
            return configuredTokenSecret;
        }

        String configuredPassword = getEffectiveLoginPassword();
        return configuredPassword.isEmpty() ? "" : "vehicle-wallpaper-admin-token:" + configuredPassword;
    }

    private long getEffectiveTokenTtlHours() {
        return Math.max(1L, adminSecurityProperties.getTokenTtlHours());
    }

    private boolean constantEquals(String expected, String actual) {
        byte[] left = normalize(expected).getBytes(StandardCharsets.UTF_8);
        byte[] right = normalize(actual).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
