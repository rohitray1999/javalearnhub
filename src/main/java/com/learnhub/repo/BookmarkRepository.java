package com.learnhub.repo;

import com.learnhub.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserIdAndLessonId(Long userId, Long lessonId);
    List<Bookmark> findByUserId(Long userId);
}
