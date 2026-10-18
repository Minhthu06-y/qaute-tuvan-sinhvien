package vn.iotstar.qaute.controller;

import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import vn.iotstar.qaute.dto.QuestionRequest;
import vn.iotstar.qaute.service.QuestionService;

@Controller
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("questionRequest", new QuestionRequest());
        return "question/create";
    }

    @PostMapping("/create")
    public String createQuestion(
            @Valid @ModelAttribute("questionRequest")
            QuestionRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "question/create";
        }

        try {
            questionService.createQuestion(
                principal.getName(), request
            );
            return "redirect:/questions/my?created";
        } catch (IllegalArgumentException
                | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return "question/create";
        }
    }

    @GetMapping("/my")
    public String myQuestions(
            Principal principal, Model model) {

        model.addAttribute(
            "questions",
            questionService.getMyQuestions(principal.getName())
        );

        return "question/my-questions";
    }
}