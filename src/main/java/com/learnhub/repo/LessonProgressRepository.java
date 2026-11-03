package com.learnhub.repo;

import com.learnhub.domain.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    boolean existsByUserIdAndLessonId(Long userId, Long lessonId);
    long countByUserIdAndCompletedAtAfter(Long userId, LocalDateTime after);

    @Query("select p.lessonId from LessonProgress p where p.userId = :u")
    List<Long> findLessonIds(@Param("u") Long userId);
}
