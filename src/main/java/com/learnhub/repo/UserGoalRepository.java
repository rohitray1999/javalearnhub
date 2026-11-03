package com.learnhub.repo;

import com.learnhub.domain.UserGoal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGoalRepository extends JpaRepository<UserGoal, Long> {
}
