package com.vehiclewallpaper.backend.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLogEntity, Long> {

    List<AdminOperationLogEntity> findTop100ByOrderByCreatedAtDesc();
}
