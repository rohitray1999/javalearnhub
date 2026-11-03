package com.learnhub.web;

import com.learnhub.repo.LearningPathRepository;
import com.learnhub.repo.LessonRepository;
import com.learnhub.repo.QuizQuestionRepository;
import com.learnhub.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final ContentService content;
    private final LearningPathRepository paths;
    private final LessonRepository lessons;
    private final QuizQuestionRepository quizzes;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("stages", content.allStages());
        model.addAttribute("paths", paths.findAllByOrderByIdAsc());
        model.addAttribute("lessonCount", lessons.count());
        model.addAttribute("quizCount", quizzes.count());
        return "home";
    }
}
