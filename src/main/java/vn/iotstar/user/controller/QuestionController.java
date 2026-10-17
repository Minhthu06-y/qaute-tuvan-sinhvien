package vn.iotstar.user.controller;

import vn.iotstar.user.dto.QuestionRequest;
import vn.iotstar.user.service.QuestionService;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/public")
    public String publicQuestions(Model model) {
        model.addAttribute("questions",
            questionService.getPublicQuestions());
        return "questions/public-list";
    }

    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("questionRequest", new QuestionRequest());
        return "questions/create";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("questionRequest")
            QuestionRequest request,
            BindingResult result,
            Authentication authentication,
            Model model) {

        if (result.hasErrors()) {
            return "questions/create";
        }

        try {
            questionService.createQuestion(
                authentication.getName(), request);
            return "redirect:/questions/my-questions?created";
        } catch (IllegalArgumentException
                 | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return "questions/create";
        }
    }

    @GetMapping("/my-questions")
    public String myQuestions(
            Authentication authentication,
            Model model) {

        model.addAttribute("questions",
            questionService.getMyQuestions(authentication.getName()));

        return "questions/my-questions";
    }
}