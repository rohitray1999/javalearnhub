package com.learnhub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bookmark",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "lessonId"}))
@Getter @Setter @NoArgsConstructor
public class Bookmark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long lessonId;

    public Bookmark(Long userId, Long lessonId) {
        this.userId = userId;
        this.lessonId = lessonId;
    }
}
