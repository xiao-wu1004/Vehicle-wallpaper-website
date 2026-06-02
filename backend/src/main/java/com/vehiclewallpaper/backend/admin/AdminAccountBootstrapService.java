package com.vehiclewallpaper.backend.admin;

import com.vehiclewallpaper.backend.config.AdminSecurityProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.time.LocalDateTime;

@Service
public class AdminAccountBootstrapService {

    private final AdminAccountRepository adminAccountRepository;
    private final AdminSecurityProperties adminSecurityProperties;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountBootstrapService(AdminAccountRepository adminAccountRepository,
                                        AdminSecurityProperties adminSecurityProperties,
                                        PasswordEncoder passwordEncoder) {
        this.adminAccountRepository = adminAccountRepository;
        this.adminSecurityProperties = adminSecurityProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void ensureBootstrapAccount() {
        String username = normalize(adminSecurityProperties.getLoginUsername());
        String password = normalize(adminSecurityProperties.getLoginPassword());

        if (username.isEmpty() || password.isEmpty()) {
            return;
        }

        AdminAccountEntity account = adminAccountRepository.findByUsernameIgnoreCase(username).orElseGet(AdminAccountEntity::new);
        LocalDateTime now = LocalDateTime.now();
        account.setUsername(username);
        account.setDisplayName(resolveDisplayName(username));
        account.setActive(true);

        String existingPasswordHash = normalize(account.getPasswordHash());
        if (existingPasswordHash.isEmpty() || !passwordEncoder.matches(password, existingPasswordHash)) {
            account.setPasswordHash(passwordEncoder.encode(password));
            account.setPasswordUpdatedAt(now);
        }

        if (account.getSessionVersion() < 1L) {
            account.setSessionVersion(1L);
        }

        adminAccountRepository.save(account);
    }

    private String resolveDisplayName(String username) {
        String configuredDisplayName = normalize(adminSecurityProperties.getLoginDisplayName());
        return configuredDisplayName.isEmpty() ? username : configuredDisplayName;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
