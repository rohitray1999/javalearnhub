package com.learnhub.web;

import com.learnhub.config.AppUserDetails;
import com.learnhub.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class QuizApiController {
    private final QuizService quiz;

    /** Body: { "<questionId>": <chosenOptionIndex>, ... }. Login ho to attempt save hota hai. */
    @PostMapping("/api/quiz/{lessonId}/submit")
    public QuizService.Result submit(@PathVariable Long lessonId,
                                     @RequestBody Map<Long, Integer> answers,
                                     @AuthenticationPrincipal AppUserDetails user) {
        return quiz.submit(lessonId, answers, user == null ? null : user.getId());
    }
}
