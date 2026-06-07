package com.vehiclewallpaper.backend.user;

import com.vehiclewallpaper.backend.config.UserAuthProperties;
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
import java.util.Locale;

@Service
public class UserAuthenticationService {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 10;
    private static final int ACCOUNT_LOCK_MINUTES = 30;
    private static final long SESSION_TOUCH_INTERVAL_SECONDS = 300L;

    private final UserAuthProperties userAuthProperties;
    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthRateLimiter rateLimiter;
    private final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();
    private final SecureRandom secureRandom = new SecureRandom();

    public UserAuthenticationService(UserAuthProperties userAuthProperties,
                                     UserAccountRepository userAccountRepository,
                                     UserSessionRepository userSessionRepository,
                                     PasswordEncoder passwordEncoder,
                                     AuthRateLimiter rateLimiter) {
        this.userAuthProperties = userAuthProperties;
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public UserAuthSessionResponse register(UserAuthRegisterRequest request, HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        String email = normalizeEmail(request.getEmail());
        String rawPassword = normalize(request.getPassword());

        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }

        // IP 限流：每个 IP 每 15 分钟最多注册 3 次
        if (!rateLimiter.allowRegistration(clientIp)) {
            throw new IllegalArgumentException("Too many registration attempts. Please try again later.");
        }

        // 密码强度：≥8位 + 至少含字母和数字
        if (rawPassword.length() < 8 || !rawPassword.matches(".*[A-Za-z].*") || !rawPassword.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must be at least 8 characters and contain both letters and numbers.");
        }

        if (userAccountRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("This email is already registered.");
        }

        UserAccountEntity account = new UserAccountEntity();
        account.setEmail(email);
        account.setDisplayName(normalizeDisplayName(request.getDisplayName(), email));
        account.setPasswordHash(passwordEncoder.encode(normalize(request.getPassword())));
        account.setPublicKey(generatePublicKey());
        account.setActive(true);

        applyLoginMetadata(account, servletRequest);
        account = userAccountRepository.save(account);
        return issueSession(account, servletRequest);
    }

    @Transactional
    public UserAuthSessionResponse login(UserAuthLoginRequest request, HttpServletRequest servletRequest) {
        String clientIp = extractClientIp(servletRequest);
        String email = normalizeEmail(request.getEmail());
        String password = normalize(request.getPassword());

        // IP 限流：每个 IP 每 15 分钟最多 5 次登录尝试
        if (!rateLimiter.allowLogin(clientIp)) {
            int remaining = rateLimiter.remainingLoginAttempts(clientIp);
            throw new UserUnauthorizedException("Too many login attempts. Please wait 15 minutes. (" + remaining + " remaining)");
        }

        UserAccountEntity account = userAccountRepository.findByEmailIgnoreCase(email).orElse(null);

        // 账号锁定检查
        if (account != null && account.getLockedUntil() != null && account.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UserUnauthorizedException("This account is temporarily locked due to repeated failed login attempts. Please try again later or contact the administrator.");
        }

        // 登录失败
        if (account == null || !account.isActive() || !passwordEncoder.matches(password, account.getPasswordHash())) {
            if (account != null) {
                account.setFailedLoginAttempts(account.getFailedLoginAttempts() + 1);
                if (account.getFailedLoginAttempts() >= MAX_FAILED_LOGIN_ATTEMPTS) {
                    account.setLockedUntil(LocalDateTime.now().plusMinutes(ACCOUNT_LOCK_MINUTES));
                }
                userAccountRepository.save(account);
            }
            throw new UserUnauthorizedException("Incorrect email or password.");
        }

        // 登录成功：重置失败计数 + 解除锁定
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        applyLoginMetadata(account, servletRequest);
        userAccountRepository.save(account);
        return issueSession(account, servletRequest);
    }

    @Transactional
    public UserAuthStatusResponse getStatus(HttpServletRequest request) {
        UserIdentity identity = authenticateOptional(request);
        if (identity == null) {
            return new UserAuthStatusResponse(false, "", "", null);
        }
        return new UserAuthStatusResponse(true, identity.getDisplayName(), identity.getEmail(), identity.getExpiresAt());
    }

    @Transactional
    public UserAuthStatusResponse logout(HttpServletRequest request) {
        UserIdentity identity = authenticateOptional(request);
        if (identity != null && identity.getSessionId() != null) {
            userSessionRepository.revokeById(identity.getSessionId(), LocalDateTime.now());
        }
        return new UserAuthStatusResponse(false, "", "", null);
    }

    @Transactional
    public UserIdentity authenticateOptional(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String authorization = normalize(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }

        String rawToken = authorization.substring(7).trim();
        if (rawToken.isEmpty()) {
            return null;
        }

        UserSessionEntity session = userSessionRepository.findWithAccountByTokenHash(hashToken(rawToken)).orElse(null);
        if (session == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(now)) {
            return null;
        }

        UserAccountEntity account = session.getAccount();
        if (account == null || !account.isActive()) {
            return null;
        }

        if (shouldRefreshLastSeen(session.getLastSeenAt(), now)) {
            session.setLastSeenAt(now);
            userSessionRepository.save(session);
        }

        return new UserIdentity(
            account.getId(),
            session.getId(),
            account.getPublicKey(),
            account.getDisplayName(),
            account.getEmail(),
            session.getExpiresAt()
        );
    }

    private UserAuthSessionResponse issueSession(UserAccountEntity account, HttpServletRequest request) {
        LocalDateTime now = LocalDateTime.now();
        UserSessionEntity session = new UserSessionEntity();
        session.setAccount(account);
        session.setIssuedAt(now);
        session.setLastSeenAt(now);
        session.setExpiresAt(now.plusHours(getEffectiveTokenTtlHours()));
        session.setIssuedIp(extractClientIp(request));
        session.setIssuedUserAgent(extractUserAgent(request));

        String accessToken = generateRawToken();
        session.setTokenHash(hashToken(accessToken));
        session = userSessionRepository.save(session);

        return new UserAuthSessionResponse(
            true,
            account.getDisplayName(),
            account.getEmail(),
            accessToken,
            session.getExpiresAt()
        );
    }

    private void applyLoginMetadata(UserAccountEntity account, HttpServletRequest request) {
        account.setLastLoginAt(LocalDateTime.now());
        account.setLastLoginIp(extractClientIp(request));
        account.setLastLoginUserAgent(extractUserAgent(request));
    }

    private boolean shouldRefreshLastSeen(LocalDateTime previousLastSeenAt, LocalDateTime now) {
        return previousLastSeenAt == null || previousLastSeenAt.isBefore(now.minusSeconds(SESSION_TOUCH_INTERVAL_SECONDS));
    }

    private long getEffectiveTokenTtlHours() {
        return Math.max(1L, userAuthProperties.getTokenTtlHours());
    }

    private String generatePublicKey() {
        byte[] bytes = new byte[18];
        secureRandom.nextBytes(bytes);
        StringBuilder builder = new StringBuilder("member_");
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return base64UrlEncoder.encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        String secret = normalize(userAuthProperties.getTokenSecret());
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
            throw new IllegalStateException("Unable to hash the user session token.", exception);
        }
    }

    private String normalizeDisplayName(String value, String email) {
        String normalized = normalize(value);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String normalizeEmail(String email) {
        return normalize(email).toLowerCase(Locale.ROOT);
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = normalize(request.getHeader("X-Forwarded-For"));
        String clientIp = forwardedFor;
        if (clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }
        if (clientIp.isEmpty()) {
            clientIp = normalize(request.getRemoteAddr());
        }
        return clientIp.isEmpty() ? "unknown" : clientIp;
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String userAgent = normalize(request.getHeader("User-Agent"));
        return userAgent.isEmpty() ? "unknown" : userAgent;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
