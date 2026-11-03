package com.learnhub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempt")
@Getter @Setter @NoArgsConstructor
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long lessonId;
    private int score;
    private int total;
    private LocalDateTime attemptedAt = LocalDateTime.now();

    public QuizAttempt(Long userId, Long lessonId, int score, int total) {
        this.userId = userId;
        this.lessonId = lessonId;
        this.score = score;
        this.total = total;
    }
}
