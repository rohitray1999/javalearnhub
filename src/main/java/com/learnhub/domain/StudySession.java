package com.learnhub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "study_session",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "studyDate"}))
@Getter @Setter @NoArgsConstructor
public class StudySession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private LocalDate studyDate;
    private int minutes;

    public StudySession(Long userId, LocalDate studyDate, int minutes) {
        this.userId = userId;
        this.studyDate = studyDate;
        this.minutes = minutes;
    }
}
