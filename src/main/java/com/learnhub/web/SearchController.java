package com.learnhub.web;

import com.learnhub.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SearchController {
    private final ContentService content;

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("results", content.search(q));
        return "search";
    }
}
