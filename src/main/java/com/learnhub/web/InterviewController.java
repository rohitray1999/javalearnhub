package com.learnhub.web;

import com.learnhub.domain.InterviewQuestion;
import com.learnhub.repo.InterviewQuestionRepository;
import com.learnhub.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class InterviewController {
    private static final List<String> LEVELS = List.of("0-1", "1-3", "3-5", "5+");

    private final InterviewQuestionRepository questions;
    private final ContentService content;

    public record Item(InterviewQuestion q, String answerHtml) {
        public String badgeClass() {
            int r = q.getLevelRank() == null ? 4 : q.getLevelRank();
            return switch (r) {
                case 1 -> "text-bg-success";
                case 2 -> "text-bg-info";
                case 3 -> "text-bg-warning";
                default -> "text-bg-danger";
            };
        }
    }

    @GetMapping("/interview")
    public String list(@RequestParam(defaultValue = "") String level,
                       @RequestParam(defaultValue = "") String topic,
                       Model model) {
        List<InterviewQuestion> all = questions.findAllByOrderByTopicOrderAscLevelRankAscSortOrderAsc();

        Map<String, Long> topicCounts = new LinkedHashMap<>();
        for (InterviewQuestion q : all) topicCounts.merge(q.getTopic(), 1L, Long::sum);

        String lvl = LEVELS.contains(level) ? level : "";
        String tpc = topicCounts.containsKey(topic) ? topic : "";

        Map<String, List<Item>> byTopic = new LinkedHashMap<>();
        int shown = 0;
        for (InterviewQuestion q : all) {
            if (!lvl.isEmpty() && !lvl.equals(q.getExperience())) continue;
            if (!tpc.isEmpty() && !tpc.equals(q.getTopic())) continue;
            byTopic.computeIfAbsent(q.getTopic(), k -> new ArrayList<>())
                    .add(new Item(q, content.render(q.getAnswerMd())));
            shown++;
        }

        model.addAttribute("byTopic", byTopic);
        model.addAttribute("levels", LEVELS);
        model.addAttribute("topics", topicCounts.keySet());
        model.addAttribute("level", lvl);
        model.addAttribute("topic", tpc);
        model.addAttribute("shown", shown);
        model.addAttribute("total", all.size());
        return "interview";
    }
}
