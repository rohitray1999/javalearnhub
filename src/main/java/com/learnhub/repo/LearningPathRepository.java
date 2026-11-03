package com.learnhub.repo;

import com.learnhub.domain.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    Optional<LearningPath> findBySlug(String slug);
    List<LearningPath> findAllByOrderByIdAsc();
}
