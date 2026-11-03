package com.learnhub.repo;

import com.learnhub.domain.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    Optional<StudySession> findByUserIdAndStudyDate(Long userId, LocalDate date);

    @Query("select s.studyDate from StudySession s where s.userId = :u")
    List<LocalDate> findDates(@Param("u") Long userId);

    @Query("select coalesce(sum(s.minutes), 0L) from StudySession s where s.userId = :u and s.studyDate >= :from")
    Long minutesSince(@Param("u") Long userId, @Param("from") LocalDate from);
}
