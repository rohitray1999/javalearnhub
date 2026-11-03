package com.learnhub.service;

import com.learnhub.domain.Lesson;
import com.learnhub.domain.Stage;
import com.learnhub.repo.LessonRepository;
import com.learnhub.repo.StageRepository;
import lombok.RequiredArgsConstructor;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentService {
    private final StageRepository stages;
    private final LessonRepository lessons;

    private final Parser parser = Parser.builder()
            .extensions(List.of(TablesExtension.create())).build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(List.of(TablesExtension.create()))
            .escapeHtml(true).build();

    public record LessonNav(Lesson prev, Lesson next) {}

    public List<Stage> allStages() {
        return stages.findAllByOrderByOrderNoAsc();
    }

    public List<Lesson> allLessons() {
        return lessons.findAllOrdered();
    }

    public Lesson lessonBySlug(String slug) {
        return lessons.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
    }

    public List<Lesson> search(String q) {
        if (q == null || q.isBlank()) return List.of();
        return lessons.search(q.trim());
    }

    public String render(String markdown) {
        return renderer.render(parser.parse(markdown == null ? "" : markdown));
    }

    public LessonNav nav(Lesson lesson) {
        List<Lesson> all = allLessons();
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(lesson.getId())) { idx = i; break; }
        }
        Lesson prev = idx > 0 ? all.get(idx - 1) : null;
        Lesson next = (idx >= 0 && idx < all.size() - 1) ? all.get(idx + 1) : null;
        return new LessonNav(prev, next);
    }
}
