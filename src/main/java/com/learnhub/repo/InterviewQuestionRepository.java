package com.learnhub.repo;

import com.learnhub.domain.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    Optional<InterviewQuestion> findByQuestion(String question);

    List<InterviewQuestion> findAllByOrderByTopicOrderAscLevelRankAscSortOrderAsc();
}
