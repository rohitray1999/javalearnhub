package com.learnhub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lesson")
@Getter @Setter @NoArgsConstructor
public class Lesson {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String contentMd;

    private Integer minJavaVersion;
    private int readMinutes = 5;
    private int orderNo;
}
