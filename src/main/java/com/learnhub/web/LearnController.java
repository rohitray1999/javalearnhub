package com.learnhub.web;

import com.learnhub.config.AppUserDetails;
import com.learnhub.domain.Lesson;
import com.learnhub.service.ContentService;
import com.learnhub.service.ProgressService;
import com.learnhub.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequiredArgsConstructor
public class LearnController {
    private final ContentService content;
    private final ProgressService progress;
    private final QuizService quiz;

    @GetMapping("/learn")
    public String index(@AuthenticationPrincipal AppUserDetails user, Model model) {
        Long uid = user == null ? null : user.getId();
        model.addAttribute("stages", content.allStages());
        model.addAttribute("done", progress.completedIds(uid));
        return "learn";
    }

    @GetMapping("/learn/{slug}")
    public String lesson(@PathVariable String slug,
                         @AuthenticationPrincipal AppUserDetails user, Model model) {
        Lesson lesson = content.lessonBySlug(slug);
        Long uid = user == null ? null : user.getId();
        Set<Long> done = progress.completedIds(uid);

        model.addAttribute("lesson", lesson);
        model.addAttribute("stage", lesson.getStage());
        model.addAttribute("html", content.render(lesson.getContentMd()));
        model.addAttribute("nav", content.nav(lesson));
        model.addAttribute("questions", quiz.questionsFor(lesson.getId()));
        model.addAttribute("done", done);
        model.addAttribute("isDone", done.contains(lesson.getId()));
        if (uid != null) {
            model.addAttribute("bookmarked", progress.isBookmarked(uid, lesson.getId()));
            model.addAttribute("note", progress.note(uid, lesson.getId()));
        }
        return "lesson";
    }

    @PostMapping("/learn/{slug}/complete")
    public String complete(@PathVariable String slug,
                           @AuthenticationPrincipal AppUserDetails user, RedirectAttributes ra) {
        progress.complete(user.getId(), content.lessonBySlug(slug));
        ra.addFlashAttribute("flash", "Lesson marked as complete.");
        return "redirect:/learn/" + slug;
    }

    @PostMapping("/learn/{slug}/bookmark")
    public String bookmark(@PathVariable String slug,
                           @AuthenticationPrincipal AppUserDetails user, RedirectAttributes ra) {
        boolean on = progress.toggleBookmark(user.getId(), content.lessonBySlug(slug).getId());
        ra.addFlashAttribute("flash", on ? "Bookmark saved." : "Bookmark removed.");
        return "redirect:/learn/" + slug;
    }

    @PostMapping("/learn/{slug}/note")
    public String note(@PathVariable String slug, @RequestParam String note,
                       @AuthenticationPrincipal AppUserDetails user, RedirectAttributes ra) {
        progress.saveNote(user.getId(), content.lessonBySlug(slug).getId(), note);
        ra.addFlashAttribute("flash", "Note saved.");
        return "redirect:/learn/" + slug;
    }
}
