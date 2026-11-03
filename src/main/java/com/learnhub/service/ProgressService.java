package com.learnhub.service;

import com.learnhub.domain.*;
import com.learnhub.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProgressService {
    private final LessonProgressRepository progress;
    private final StudySessionRepository sessions;
    private final UserGoalRepository goals;
    private final BookmarkRepository bookmarks;
    private final LessonNoteRepository notes;

    // ---------- completion ----------
    @Transactional
    public void complete(Long userId, Lesson lesson) {
        if (!progress.existsByUserIdAndLessonId(userId, lesson.getId())) {
            progress.save(new LessonProgress(userId, lesson.getId()));
            addMinutes(userId, Math.max(1, lesson.getReadMinutes()));
        }
    }

    @Transactional
    public void addMinutes(Long userId, int minutes) {
        LocalDate today = LocalDate.now();
        StudySession s = sessions.findByUserIdAndStudyDate(userId, today)
                .orElseGet(() -> new StudySession(userId, today, 0));
        s.setMinutes(s.getMinutes() + minutes);
        sessions.save(s);
    }

    public Set<Long> completedIds(Long userId) {
        if (userId == null) return Set.of();
        return new HashSet<>(progress.findLessonIds(userId));
    }

    // ---------- streak / stats ----------
    public int streak(Long userId) {
        Set<LocalDate> days = new HashSet<>(sessions.findDates(userId));
        LocalDate d = days.contains(LocalDate.now()) ? LocalDate.now() : LocalDate.now().minusDays(1);
        int streak = 0;
        while (days.contains(d)) {
            streak++;
            d = d.minusDays(1);
        }
        return streak;
    }

    public long minutesLast7Days(Long userId) {
        return sessions.minutesSince(userId, LocalDate.now().minusDays(6));
    }

    public long minutesToday(Long userId) {
        return sessions.findByUserIdAndStudyDate(userId, LocalDate.now())
                .map(StudySession::getMinutes).orElse(0);
    }

    /** User ki apni last-14-days speed se finish date. */
    public Optional<LocalDate> estimateFinish(Long userId, long remainingLessons) {
        if (remainingLessons <= 0) return Optional.of(LocalDate.now());
        long recent = progress.countByUserIdAndCompletedAtAfter(userId, LocalDateTime.now().minusDays(14));
        if (recent == 0) return Optional.empty();
        double perDay = recent / 14.0;
        return Optional.of(LocalDate.now().plusDays((long) Math.ceil(remainingLessons / perDay)));
    }

    // ---------- goal ----------
    public UserGoal goal(Long userId) {
        return goals.findById(userId).orElseGet(() -> new UserGoal(userId));
    }

    @Transactional
    public void saveGoal(Long userId, int dailyMinutes, LocalDate target) {
        UserGoal g = goals.findById(userId).orElseGet(() -> new UserGoal(userId));
        g.setDailyMinutes(Math.min(Math.max(dailyMinutes, 5), 600));
        g.setTargetDate(target);
        goals.save(g);
    }

    // ---------- bookmarks ----------
    public boolean isBookmarked(Long userId, Long lessonId) {
        return bookmarks.findByUserIdAndLessonId(userId, lessonId).isPresent();
    }

    public Set<Long> bookmarkedIds(Long userId) {
        Set<Long> ids = new LinkedHashSet<>();
        bookmarks.findByUserId(userId).forEach(b -> ids.add(b.getLessonId()));
        return ids;
    }

    @Transactional
    public boolean toggleBookmark(Long userId, Long lessonId) {
        Optional<Bookmark> existing = bookmarks.findByUserIdAndLessonId(userId, lessonId);
        if (existing.isPresent()) {
            bookmarks.delete(existing.get());
            return false;
        }
        bookmarks.save(new Bookmark(userId, lessonId));
        return true;
    }

    // ---------- notes ----------
    public String note(Long userId, Long lessonId) {
        return notes.findByUserIdAndLessonId(userId, lessonId).map(LessonNote::getNote).orElse("");
    }

    @Transactional
    public void saveNote(Long userId, Long lessonId, String text) {
        LessonNote n = notes.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> new LessonNote(userId, lessonId));
        n.setNote(text == null ? "" : text);
        n.setUpdatedAt(LocalDateTime.now());
        notes.save(n);
    }
}
