package vn.iotstar.controller;

import java.security.Principal;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.service.QuestionModerationService;

@Controller
@RequestMapping("/manager/question-reports")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerQuestionReportController {
    private final QuestionModerationService moderation;

    @GetMapping
    public String page(Principal principal, Model model) {
        model.addAttribute("reports", moderation.pendingForManager(principal.getName()));
        return "manager/question-reports";
    }

    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable Long id, @RequestParam(required = false) String note,
            Principal principal, RedirectAttributes flash) {
        moderation.decide(id, principal.getName(), false, note);
        flash.addFlashAttribute("msg", "Đã giữ lại câu hỏi và đóng báo cáo.");
        return "redirect:/manager/question-reports";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @RequestParam(required = false) String note,
            Principal principal, RedirectAttributes flash) {
        moderation.decide(id, principal.getName(), true, note);
        flash.addFlashAttribute("msg", "Đã gỡ câu hỏi khỏi hệ thống.");
        return "redirect:/manager/question-reports";
    }
}
