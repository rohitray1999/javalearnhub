package com.learnhub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "lessonId"}))
@Getter @Setter @NoArgsConstructor
public class LessonProgress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long lessonId;
    private LocalDateTime completedAt = LocalDateTime.now();

    public LessonProgress(Long userId, Long lessonId) {
        this.userId = userId;
        this.lessonId = lessonId;
    }
}
