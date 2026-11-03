package com.learnhub.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "user_goal")
@Getter @Setter @NoArgsConstructor
public class UserGoal {
    @Id
    private Long userId;
    private int dailyMinutes = 20;
    private LocalDate targetDate;

    public UserGoal(Long userId) {
        this.userId = userId;
    }
}
