package vn.iotstar.qaute.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import vn.iotstar.qaute.service.QuestionService;

@Controller
public class ForumController {

    private final QuestionService questionService;

    public ForumController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/forum")
    public String forum(Model model) {
        model.addAttribute(
            "questions",
            questionService.getPublicQuestions()
        );
        return "forum/index";
    }
}