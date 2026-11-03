package com.learnhub.repo;

import com.learnhub.domain.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findTop5ByUserIdOrderByAttemptedAtDesc(Long userId);
}
