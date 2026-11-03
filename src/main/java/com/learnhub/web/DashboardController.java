package com.learnhub.web;

import com.learnhub.config.AppUserDetails;
import com.learnhub.domain.Lesson;
import com.learnhub.repo.QuizAttemptRepository;
import com.learnhub.service.ContentService;
import com.learnhub.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final ContentService content;
    private final ProgressService progress;
    private final QuizAttemptRepository attempts;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserDetails user, Model model) {
        Long uid = user.getId();
        List<Lesson> all = content.allLessons();
        Set<Long> done = progress.completedIds(uid);
        long finished = all.stream().filter(l -> done.contains(l.getId())).count();
        long remaining = all.size() - finished;

        model.addAttribute("total", all.size());
        model.addAttribute("finished", finished);
        model.addAttribute("percent", all.isEmpty() ? 0 : (int) (finished * 100 / all.size()));
        model.addAttribute("streak", progress.streak(uid));
        model.addAttribute("minutesToday", progress.minutesToday(uid));
        model.addAttribute("minutesWeek", progress.minutesLast7Days(uid));
        model.addAttribute("goal", progress.goal(uid));
        model.addAttribute("finishDate", progress.estimateFinish(uid, remaining).orElse(null));
        model.addAttribute("resume", all.stream().filter(l -> !done.contains(l.getId())).findFirst().orElse(null));

        Set<Long> bookmarkIds = progress.bookmarkedIds(uid);
        model.addAttribute("bookmarks", all.stream().filter(l -> bookmarkIds.contains(l.getId())).toList());
        model.addAttribute("attempts", attempts.findTop5ByUserIdOrderByAttemptedAtDesc(uid));
        model.addAttribute("lessonById", all.stream()
                .collect(java.util.stream.Collectors.toMap(Lesson::getId, l -> l)));
        return "dashboard";
    }

    @PostMapping("/dashboard/goal")
    public String saveGoal(@RequestParam int dailyMinutes,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
                           @AuthenticationPrincipal AppUserDetails user, RedirectAttributes ra) {
        progress.saveGoal(user.getId(), dailyMinutes, targetDate);
        ra.addFlashAttribute("flash", "Goal saved.");
        return "redirect:/dashboard";
    }
}
