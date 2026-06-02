package com.vehiclewallpaper.backend.admin;

import com.vehiclewallpaper.backend.config.AdminSecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class AdminAuthenticationService {

    private static final String AUTH_MODE_API_KEY = "API_KEY";
    private static final String AUTH_MODE_PASSWORD = "PASSWORD";
    private static final String TARGET_TYPE_AUTH = "ADMIN_AUTH";

    private final AdminSecurityProperties adminSecurityProperties;
    private final AdminAccountRepository adminAccountRepository;
    private final AdminSessionRepository adminSessionRepository;
    private final AdminOperationLogService adminOperationLogService;
    private final AdminRequestContextService adminRequestContextService;
    private final PasswordEncoder passwordEncoder;
    private final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminAuthenticationService(AdminSecurityProperties adminSecurityProperties,
                                      AdminAccountRepository adminAccountRepository,
                                      AdminSessionRepository adminSessionRepository,
                                      AdminOperationLogService adminOperationLogService,
                                      AdminRequestContextService adminRequestContextService,
                                      PasswordEncoder passwordEncoder) {
        this.adminSecurityProperties = adminSecurityProperties;
        this.adminAccountRepository = adminAccountRepository;
        this.adminSessionRepository = adminSessionRepository;
        this.adminOperationLogService = adminOperationLogService;
        this.adminRequestContextService = adminRequestContextService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AdminAuthOptionsResponse getOptions() {
        List<AdminAccountEntity> activeAccounts = adminAccountRepository.findAllByActiveTrueOrderByUsernameAsc();
        String loginUsernameHint = activeAccounts.isEmpty()
            ? normalize(adminSecurityProperties.getLoginUsername())
            : activeAccounts.get(0).getUsername();
        return new AdminAuthOptionsResponse(!activeAccounts.isEmpty(), isApiKeyEnabled(), loginUsernameHint);
    }

    @Transactional
    public AdminAuthLoginResponse login(AdminAuthLoginRequest request, HttpServletRequest servletRequest) {
        List<AdminAccountEntity> activeAccounts = adminAccountRepository.findAllByActiveTrueOrderByUsernameAsc();
        if (activeAccounts.isEmpty()) {
            throw new AdminSecurityNotConfiguredException("管理员账号登录尚未配置。");
        }

        String incomingUsername = normalize(request.getUsername());
        String incomingPassword = normalize(request.getPassword());
        AdminAccountEntity account = adminAccountRepository.findByUsernameIgnoreCase(incomingUsername).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (account == null || !account.isActive()) {
            adminOperationLogService.logAuthEvent(
                null,
                incomingUsername,
                AUTH_MODE_PASSWORD,
                "LOGIN_FAILED",
                TARGET_TYPE_AUTH,
                incomingUsername,
                "Unknown or inactive admin account.",
                servletRequest
            );
            throw new AdminUnauthorizedException("管理员账号或密码错误。");
        }

        if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(now)) {
            adminOperationLogService.logAuthEvent(
                account,
                account.getUsername(),
                AUTH_MODE_PASSWORD,
                "LOGIN_BLOCKED",
                TARGET_TYPE_AUTH,
                account.getId().toString(),
                "Admin account is temporarily locked after repeated failures.",
                servletRequest
            );
            throw new AdminUnauthorizedException("管理员账号已临时锁定，请稍后再试。");
        }

        if (!passwordEncoder.matches(incomingPassword, account.getPasswordHash())) {
            processFailedLogin(account, now, servletRequest);
            throw new AdminUnauthorizedException("管理员账号或密码错误。");
        }

        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        AdminRequestMetadata metadata = adminRequestContextService.extract(servletRequest);
        account.setLastLoginAt(now);
        account.setLastLoginIp(metadata.getIpAddress());
        account.setLastLoginUserAgent(metadata.getUserAgent());
        adminAccountRepository.save(account);

        AdminSessionEntity session = new AdminSessionEntity();
        session.setAccount(account);
        session.setAuthMode(AUTH_MODE_PASSWORD);
        session.setSessionVersion(account.getSessionVersion());
        session.setIssuedAt(now);
        session.setLastSeenAt(now);
        session.setExpiresAt(now.plusHours(getEffectiveTokenTtlHours()));
        session.setIssuedIp(metadata.getIpAddress());
        session.setIssuedUserAgent(metadata.getUserAgent());

        String accessToken = generateRawToken();
        session.setTokenHash(hashToken(accessToken));
        session = adminSessionRepository.save(session);

        adminOperationLogService.logAuthEvent(
            account,
            account.getUsername(),
            AUTH_MODE_PASSWORD,
            "LOGIN_SUCCESS",
            TARGET_TYPE_AUTH,
            session.getId().toString(),
            "Admin session created successfully.",
            servletRequest
        );
        return new AdminAuthLoginResponse(account.getUsername(), AUTH_MODE_PASSWORD, accessToken, session.getExpiresAt());
    }

    @Transactional
    public AdminActionStatusResponse logoutAll(HttpServletRequest request) {
        AdminIdentity identity = requireAuthenticatedPasswordIdentity();
        AdminAccountEntity account = adminAccountRepository.findById(identity.getAccountId())
            .orElseThrow(() -> new AdminUnauthorizedException("管理员登录凭证无效或已过期。"));

        account.setSessionVersion(account.getSessionVersion() + 1L);
        adminAccountRepository.save(account);
        int affectedCount = adminSessionRepository.revokeAllActiveByAccountId(account.getId(), LocalDateTime.now());

        adminOperationLogService.logAuthEvent(
            account,
            account.getUsername(),
            identity.getAuthMode(),
            "LOGOUT_ALL",
            TARGET_TYPE_AUTH,
            account.getId().toString(),
            "Revoked all active admin sessions.",
            request
        );
        return new AdminActionStatusResponse("All admin sessions have been revoked.", affectedCount);
    }

    @Transactional
    public AdminIdentity authenticate(HttpServletRequest request) {
        String authorization = normalize(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authenticateBearerToken(authorization.substring(7).trim(), request);
        }

        String configuredApiKey = normalize(adminSecurityProperties.getApiKey());
        String incomingApiKey = normalize(request.getHeader(adminSecurityProperties.getHeaderName()));
        if (!configuredApiKey.isEmpty() && constantEquals(configuredApiKey, incomingApiKey)) {
            return new AdminIdentity(null, null, "api-key-admin", AUTH_MODE_API_KEY);
        }

        if (!hasAnyActiveAdminAccounts() && configuredApiKey.isEmpty()) {
            throw new AdminSecurityNotConfiguredException("管理员登录或 API Key 尚未配置。");
        }

        if (!authorization.isEmpty()) {
            throw new AdminUnauthorizedException("管理员登录凭证无效或已过期。");
        }

        throw new AdminUnauthorizedException("缺少有效的管理员登录凭证。");
    }

    private AdminIdentity authenticateBearerToken(String rawToken, HttpServletRequest request) {
        if (rawToken.isEmpty()) {
            throw new AdminUnauthorizedException("缺少有效的管理员登录凭证。");
        }

        AdminSessionEntity session = adminSessionRepository.findWithAccountByTokenHash(hashToken(rawToken))
            .orElseThrow(() -> new AdminUnauthorizedException("管理员登录凭证无效或已过期。"));
        LocalDateTime now = LocalDateTime.now();

        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(now)) {
            throw new AdminUnauthorizedException("管理员登录凭证无效或已过期。");
        }

        AdminAccountEntity account = session.getAccount();
        if (account == null || !account.isActive()) {
            throw new AdminUnauthorizedException("管理员登录凭证无效或已过期。");
        }

        if (session.getSessionVersion() != account.getSessionVersion()) {
            throw new AdminUnauthorizedException("管理员登录凭证无效或已过期。");
        }

        session.setLastSeenAt(now);
        AdminRequestMetadata metadata = adminRequestContextService.extract(request);
        session.setIssuedIp(normalize(session.getIssuedIp()).isEmpty() ? metadata.getIpAddress() : session.getIssuedIp());
        adminSessionRepository.save(session);

        return new AdminIdentity(account.getId(), session.getId(), account.getUsername(), AUTH_MODE_PASSWORD);
    }

    private AdminIdentity requireAuthenticatedPasswordIdentity() {
        AdminIdentity identity = adminRequestContextService.currentIdentity();
        if (identity == null || identity.getAccountId() == null || !AUTH_MODE_PASSWORD.equals(identity.getAuthMode())) {
            throw new AdminUnauthorizedException("当前操作需要管理员账号登录。");
        }
        return identity;
    }

    private void processFailedLogin(AdminAccountEntity account, LocalDateTime now, HttpServletRequest request) {
        int nextFailedAttempts = account.getFailedLoginAttempts() + 1;
        int maxFailedAttempts = Math.max(1, adminSecurityProperties.getMaxFailedAttempts());
        account.setFailedLoginAttempts(nextFailedAttempts);

        String detail = "Incorrect password.";
        if (nextFailedAttempts >= maxFailedAttempts) {
            account.setLockedUntil(now.plusMinutes(Math.max(1L, adminSecurityProperties.getLockMinutes())));
            account.setFailedLoginAttempts(0);
            detail = "Account locked after repeated failed logins.";
        }

        adminAccountRepository.save(account);
        adminOperationLogService.logAuthEvent(
            account,
            account.getUsername(),
            AUTH_MODE_PASSWORD,
            "LOGIN_FAILED",
            TARGET_TYPE_AUTH,
            account.getId().toString(),
            detail,
            request
        );
    }

    private boolean hasAnyActiveAdminAccounts() {
        return !adminAccountRepository.findAllByActiveTrueOrderByUsernameAsc().isEmpty();
    }

    private boolean isApiKeyEnabled() {
        return !normalize(adminSecurityProperties.getApiKey()).isEmpty();
    }

    private long getEffectiveTokenTtlHours() {
        return Math.max(1L, adminSecurityProperties.getTokenTtlHours());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return base64UrlEncoder.encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        String secret = normalize(adminSecurityProperties.getTokenSecret());
        String tokenSource = secret.isEmpty() ? rawToken : secret + ":" + rawToken;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(tokenSource.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte hashedByte : hashedBytes) {
                builder.append(String.format("%02x", hashedByte));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash the admin session token.", exception);
        }
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
