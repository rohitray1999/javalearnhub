package com.learnhub.repo;

import com.learnhub.domain.LessonNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LessonNoteRepository extends JpaRepository<LessonNote, Long> {
    Optional<LessonNote> findByUserIdAndLessonId(Long userId, Long lessonId);
}
