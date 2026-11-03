package com.learnhub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_note",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "lessonId"}))
@Getter @Setter @NoArgsConstructor
public class LessonNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long lessonId;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public LessonNote(Long userId, Long lessonId) {
        this.userId = userId;
        this.lessonId = lessonId;
    }
}
