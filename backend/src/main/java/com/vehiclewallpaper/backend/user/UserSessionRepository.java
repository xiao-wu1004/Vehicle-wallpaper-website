package com.vehiclewallpaper.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {

    @Query("select session from UserSessionEntity session join fetch session.account account where session.tokenHash = :tokenHash")
    Optional<UserSessionEntity> findWithAccountByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("update UserSessionEntity session set session.revokedAt = :revokedAt where session.id = :sessionId and session.revokedAt is null")
    int revokeById(@Param("sessionId") Long sessionId, @Param("revokedAt") LocalDateTime revokedAt);
}
