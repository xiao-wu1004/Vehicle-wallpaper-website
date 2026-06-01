package com.vehiclewallpaper.backend.feedback;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

public interface FeedbackRepository extends JpaRepository<FeedbackMessage, Long> {

    boolean existsByFeaturedTrue();

    List<FeedbackMessage> findTop6ByStatusAndFeaturedTrueOrderByCreatedAtDesc(FeedbackStatus status);

    long countByStatus(FeedbackStatus status);

    long countByFeaturedTrue();

    default List<FeedbackMessage> findAllNewestFirst() {
        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
