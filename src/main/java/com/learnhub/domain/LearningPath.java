package com.learnhub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "learning_path")
@Getter @Setter @NoArgsConstructor
public class LearningPath {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    private String levelName;

    @Column(length = 500)
    private String description;

    @ManyToMany
    @JoinTable(name = "path_lesson",
            joinColumns = @JoinColumn(name = "path_id"),
            inverseJoinColumns = @JoinColumn(name = "lesson_id"))
    @OrderColumn(name = "sort_no")
    private List<Lesson> lessons = new ArrayList<>();
}
