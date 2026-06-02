package com.vehiclewallpaper.backend.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdminSessionRepository extends JpaRepository<AdminSessionEntity, Long> {

    @Query("select session from AdminSessionEntity session join fetch session.account account where session.tokenHash = :tokenHash")
    Optional<AdminSessionEntity> findWithAccountByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update AdminSessionEntity session set session.revokedAt = :revokedAt where session.account.id = :accountId and session.revokedAt is null")
    int revokeAllActiveByAccountId(@Param("accountId") Long accountId, @Param("revokedAt") LocalDateTime revokedAt);

    @Query("select session from AdminSessionEntity session join fetch session.account account where session.account.id = :accountId order by session.issuedAt desc")
    List<AdminSessionEntity> findRecentByAccountId(@Param("accountId") Long accountId);
}
