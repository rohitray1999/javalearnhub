package com.learnhub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stage")
@Getter @Setter @NoArgsConstructor
public class Stage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    private String category;
    private String levelName;

    @Column(length = 500)
    private String description;

    private int orderNo;

    @OneToMany(mappedBy = "stage")
    @OrderBy("orderNo ASC")
    private List<Lesson> lessons = new ArrayList<>();
}
