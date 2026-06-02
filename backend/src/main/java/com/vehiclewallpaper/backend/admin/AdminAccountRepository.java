package com.vehiclewallpaper.backend.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminAccountRepository extends JpaRepository<AdminAccountEntity, Long> {

    Optional<AdminAccountEntity> findByUsernameIgnoreCase(String username);

    List<AdminAccountEntity> findAllByActiveTrueOrderByUsernameAsc();
}
