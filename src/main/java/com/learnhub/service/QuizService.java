package com.learnhub.service;

import com.learnhub.domain.QuizAttempt;
import com.learnhub.domain.QuizQuestion;
import com.learnhub.repo.QuizAttemptRepository;
import com.learnhub.repo.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizQuestionRepository questions;
    private final QuizAttemptRepository attempts;

    /** Page par sahi jawab (correctIdx) kabhi nahi bhejte. */
    public record QuestionView(Long id, String question, List<String> options) {}
    public record Review(Long questionId, boolean correct, int correctIdx, String explanation) {}
    public record Result(int score, int total, List<Review> review) {}

    public List<QuestionView> questionsFor(Long lessonId) {
        return questions.findByLessonIdOrderByIdAsc(lessonId).stream()
                .map(q -> new QuestionView(q.getId(), q.getQuestion(), List.copyOf(q.getOptions())))
                .toList();
    }

    @Transactional
    public Result submit(Long lessonId, Map<Long, Integer> answers, Long userId) {
        List<QuizQuestion> qs = questions.findByLessonIdOrderByIdAsc(lessonId);
        int score = 0;
        List<Review> review = new ArrayList<>();
        for (QuizQuestion q : qs) {
            Integer chosen = answers.get(q.getId());
            boolean ok = chosen != null && chosen == q.getCorrectIdx();
            if (ok) score++;
            review.add(new Review(q.getId(), ok, q.getCorrectIdx(), q.getExplanation()));
        }
        if (userId != null && !qs.isEmpty()) {
            attempts.save(new QuizAttempt(userId, lessonId, score, qs.size()));
        }
        return new Result(score, qs.size(), review);
    }
}
