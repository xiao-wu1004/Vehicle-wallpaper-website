package com.vehiclewallpaper.backend.feedback;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<FeedbackMessage, Long> {

    boolean existsByFeaturedTrue();

    List<FeedbackMessage> findTop6ByStatusAndFeaturedTrueOrderByCreatedAtDesc(FeedbackStatus status);
}
