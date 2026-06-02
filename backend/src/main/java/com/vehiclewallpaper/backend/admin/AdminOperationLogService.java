package com.vehiclewallpaper.backend.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminOperationLogService {

    private static final String AUTH_MODE_SYSTEM = "SYSTEM";

    private final AdminOperationLogRepository adminOperationLogRepository;
    private final AdminRequestContextService adminRequestContextService;
    private final AdminAccountRepository adminAccountRepository;

    public AdminOperationLogService(AdminOperationLogRepository adminOperationLogRepository,
                                    AdminRequestContextService adminRequestContextService,
                                    AdminAccountRepository adminAccountRepository) {
        this.adminOperationLogRepository = adminOperationLogRepository;
        this.adminRequestContextService = adminRequestContextService;
        this.adminAccountRepository = adminAccountRepository;
    }

    @Transactional
    public void logCurrentAction(String action, String targetType, String targetId, String detail) {
        AdminIdentity identity = adminRequestContextService.currentIdentity();
        AdminRequestMetadata metadata = adminRequestContextService.currentMetadata();
        saveLog(resolveAccount(identity), identity == null ? "system" : identity.getUsername(),
            identity == null ? AUTH_MODE_SYSTEM : identity.getAuthMode(),
            action, targetType, targetId, detail, metadata);
    }

    @Transactional
    public void logAuthEvent(AdminAccountEntity account,
                             String actorUsername,
                             String authMode,
                             String action,
                             String targetType,
                             String targetId,
                             String detail,
                             HttpServletRequest request) {
        saveLog(account, actorUsername, authMode, action, targetType, targetId, detail, adminRequestContextService.extract(request));
    }

    @Transactional(readOnly = true)
    public List<AdminOperationLogResponse> getRecentLogs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<AdminOperationLogResponse> responses = new ArrayList<AdminOperationLogResponse>();
        List<AdminOperationLogEntity> logs = adminOperationLogRepository.findTop100ByOrderByCreatedAtDesc();
        for (AdminOperationLogEntity log : logs) {
            responses.add(new AdminOperationLogResponse(
                log.getId(),
                log.getActorUsername(),
                log.getAuthMode(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetail(),
                log.getRequestPath(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt()
            ));
            if (responses.size() >= safeLimit) {
                break;
            }
        }
        return responses;
    }

    private AdminAccountEntity resolveAccount(AdminIdentity identity) {
        if (identity == null || identity.getAccountId() == null) {
            return null;
        }
        return adminAccountRepository.findById(identity.getAccountId()).orElse(null);
    }

    private void saveLog(AdminAccountEntity account,
                         String actorUsername,
                         String authMode,
                         String action,
                         String targetType,
                         String targetId,
                         String detail,
                         AdminRequestMetadata metadata) {
        AdminOperationLogEntity entity = new AdminOperationLogEntity();
        entity.setAccount(account);
        entity.setActorUsername(normalize(actorUsername, "unknown"));
        entity.setAuthMode(normalize(authMode, AUTH_MODE_SYSTEM));
        entity.setAction(normalize(action, "UNKNOWN_ACTION"));
        entity.setTargetType(normalize(targetType, "unknown"));
        entity.setTargetId(normalize(targetId, ""));
        entity.setDetail(normalize(detail, ""));
        entity.setRequestPath(metadata.getRequestPath());
        entity.setIpAddress(metadata.getIpAddress());
        entity.setUserAgent(metadata.getUserAgent());
        adminOperationLogRepository.save(entity);
    }

    private String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
