package com.learnhub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_question")
@Getter @Setter @NoArgsConstructor
public class QuizQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(length = 500)
    private String question;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quiz_option", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "opt_no")
    @Column(name = "option_text", length = 300)
    private List<String> options = new ArrayList<>();

    private int correctIdx;

    @Column(length = 1000)
    private String explanation;
}
