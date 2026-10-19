package vn.iotstar.qaute.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.iotstar.qaute.dto.QuestionRequest;
import vn.iotstar.qaute.entity.Question;
import vn.iotstar.qaute.service.QuestionService;

@RestController("questionApiController")
@RequestMapping("/api/qaute/questions")
public class QuestionController {
    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/public")
    public List<QuestionSummary> publicQuestions() {
        return questionService.getPublicQuestions().stream()
            .map(QuestionSummary::from)
            .toList();
    }

    @GetMapping("/my")
    public List<QuestionSummary> myQuestions(Principal principal) {
        return questionService.getMyQuestions(principal.getName()).stream()
            .map(QuestionSummary::from)
            .toList();
    }

    @PostMapping
    public ResponseEntity<Void> createQuestion(
        Principal principal,
        @Valid @RequestBody QuestionRequest request
    ) {
        questionService.createQuestion(principal.getName(), request);
        return ResponseEntity.accepted().build();
    }

    public record QuestionSummary(
        Long id,
        String title,
        String content,
        String category,
        String status,
        boolean publicVisible,
        LocalDateTime createdAt
    ) {
        static QuestionSummary from(Question question) {
            return new QuestionSummary(
                question.getId(),
                question.getTitle(),
                question.getContent(),
                question.getCategory(),
                question.getStatus().name(),
                question.isPublicVisible(),
                question.getCreatedAt()
            );
        }
    }
}

