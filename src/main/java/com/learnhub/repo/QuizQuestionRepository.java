package com.learnhub.repo;

import com.learnhub.domain.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByLessonIdOrderByIdAsc(Long lessonId);
}
