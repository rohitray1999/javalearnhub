package com.learnhub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interview_question")
@Getter @Setter @NoArgsConstructor
public class InterviewQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;
    private Integer topicOrder;

    @Column(length = 500)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answerMd;

    /** One of: 0-1, 1-3, 3-5, 5+ (years of experience). */
    private String experience;
    private Integer levelRank;
    private Integer sortOrder;
}
