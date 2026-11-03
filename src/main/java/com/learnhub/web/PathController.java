package com.learnhub.web;

import com.learnhub.config.AppUserDetails;
import com.learnhub.domain.LearningPath;
import com.learnhub.repo.LearningPathRepository;
import com.learnhub.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class PathController {
    private final LearningPathRepository paths;
    private final ProgressService progress;

    @GetMapping("/paths")
    public String list(Model model) {
        model.addAttribute("paths", paths.findAllByOrderByIdAsc());
        return "paths";
    }

    @GetMapping("/paths/{slug}")
    public String detail(@PathVariable String slug,
                         @AuthenticationPrincipal AppUserDetails user, Model model) {
        LearningPath path = paths.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Path not found"));
        var done = progress.completedIds(user == null ? null : user.getId());
        long finished = path.getLessons().stream().filter(l -> done.contains(l.getId())).count();
        int total = path.getLessons().size();
        model.addAttribute("path", path);
        model.addAttribute("done", done);
        model.addAttribute("finished", finished);
        model.addAttribute("percent", total == 0 ? 0 : (int) (finished * 100 / total));
        return "path";
    }
}
