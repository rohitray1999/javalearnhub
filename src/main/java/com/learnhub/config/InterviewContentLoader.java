package com.learnhub.config;

import com.learnhub.domain.InterviewQuestion;
import com.learnhub.repo.InterviewQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interview content ko Markdown files se sync karta hai (content/interview/*.md).
 *
 * File format:
 *   ---
 *   topic: Core Java
 *   order: 1
 *   ---
 *   ## [0-1] Question text?
 *   Answer in Markdown...
 *
 * Levels: [0-1], [1-3], [3-5], [5+]. Har restart par sync hota hai:
 * naya question add, badla hua update, file se hata hua DB se delete.
 */
@Component
@RequiredArgsConstructor
public class InterviewContentLoader implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(InterviewContentLoader.class);
    private static final Pattern HEADING = Pattern.compile("^##\\s+\\[(0-1|1-3|3-5|5\\+)]\\s+(.+)$");

    private final InterviewQuestionRepository interviews;

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        Resource[] files = new PathMatchingResourcePatternResolver()
                .getResources("classpath:content/interview/*.md");
        Arrays.sort(files, Comparator.comparing(r -> String.valueOf(r.getFilename())));

        Set<String> seen = new HashSet<>();
        for (Resource file : files) {
            parse(file, seen);
        }
        if (!seen.isEmpty()) {
            interviews.findAll().stream()
                    .filter(q -> !seen.contains(q.getQuestion()))
                    .forEach(interviews::delete);
        }
        log.info("Interview content synced: {} questions from {} files", seen.size(), files.length);
    }

    private void parse(Resource file, Set<String> seen) throws IOException {
        String text = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String[] lines = text.split("\\R", -1);

        String topic = file.getFilename();
        int topicOrder = 99;
        int i = 0;
        if (lines.length > 0 && lines[0].trim().equals("---")) {
            i = 1;
            while (i < lines.length && !lines[i].trim().equals("---")) {
                String[] kv = lines[i].split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String val = kv[1].trim();
                    if (key.equals("topic")) topic = val;
                    if (key.equals("order")) topicOrder = Integer.parseInt(val);
                }
                i++;
            }
            i++; // skip closing ---
        }

        String level = null;
        String question = null;
        StringBuilder answer = new StringBuilder();
        int sort = 0;

        for (; i < lines.length; i++) {
            Matcher m = HEADING.matcher(lines[i]);
            if (m.matches()) {
                if (question != null) save(topic, topicOrder, level, question, answer.toString(), sort++, seen);
                level = m.group(1);
                question = m.group(2).trim();
                answer.setLength(0);
            } else if (question != null) {
                answer.append(lines[i]).append('\n');
            }
        }
        if (question != null) save(topic, topicOrder, level, question, answer.toString(), sort, seen);
    }

    private void save(String topic, int topicOrder, String level, String question,
                      String answer, int sort, Set<String> seen) {
        InterviewQuestion q = interviews.findByQuestion(question).orElseGet(InterviewQuestion::new);
        q.setTopic(topic);
        q.setTopicOrder(topicOrder);
        q.setQuestion(question);
        q.setAnswerMd(answer.trim());
        q.setExperience(level);
        q.setLevelRank(rank(level));
        q.setSortOrder(sort);
        interviews.save(q);
        seen.add(question);
    }

    private static int rank(String level) {
        return switch (level) {
            case "0-1" -> 1;
            case "1-3" -> 2;
            case "3-5" -> 3;
            default -> 4;
        };
    }
}
