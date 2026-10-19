package vn.iotstar.controller;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import vn.iotstar.model.*;
import vn.iotstar.repository.AnswerRepository;
import vn.iotstar.repository.QuestionRepository;
import vn.iotstar.service.NotificationService;
import vn.iotstar.service.SessionContext;

@Controller
public class ManagerController {
    private final AnswerRepository answers;
    private final QuestionRepository questions;
    private final SessionContext sessionContext;
    private final NotificationService notifications;
    public ManagerController(AnswerRepository answers, QuestionRepository questions, SessionContext sessionContext, NotificationService notifications) {
        this.answers = answers; this.questions = questions; this.sessionContext = sessionContext; this.notifications = notifications;
    }
    @GetMapping("/manager/approvals") public String approvals(HttpSession session, Model model) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (!viewer.isManager()) return "redirect:/questions";
        List<Answer> pending = answers.findByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus.PENDING);
        model.addAttribute("pendingAnswers", pending); return "approvals";
    }
    @PostMapping("/manager/answers/{id}/review") public String review(@PathVariable Long id, @RequestParam boolean approved,
            @RequestParam String reason, HttpSession session, RedirectAttributes flash) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (!viewer.isManager()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Answer answer = answers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Question question = answer.getQuestion();
        if (answer.getApprovalStatus() != ApprovalStatus.PENDING) { flash.addFlashAttribute("error", "Yêu cầu duyệt này đã được xử lý."); return "redirect:/manager/approvals"; }
        if (reason.isBlank()) { flash.addFlashAttribute("error", "Vui lòng ghi lý do duyệt hoặc từ chối."); return "redirect:/manager/approvals"; }
        answer.setApprovalStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        answer.setReviewReason(reason.trim()); answers.save(answer);
        if (approved) {
            question.setStatus(QuestionStatus.ANSWERED); question.setReplyNotification(true); questions.save(question);
        } else if (!answers.existsByQuestionIdAndApprovalStatus(question.getId(), ApprovalStatus.APPROVED)
                && !answers.existsByQuestionIdAndApprovalStatus(question.getId(), ApprovalStatus.NOT_REQUIRED)) {
            question.setStatus(QuestionStatus.OPEN); questions.save(question);
        }
        String message = approved ? "Câu trả lời đã được duyệt: " + reason.trim() : "Câu trả lời chưa được duyệt: " + reason.trim();
        notifications.notifyUser(question.getAsker().getId(), "review", message, question.getId());
        notifications.notifyUser(answer.getAuthor().getId(), "review", message, question.getId());
        flash.addFlashAttribute("success", approved ? "Đã duyệt và thông báo cho người hỏi." : "Đã từ chối và gửi lý do.");
        return "redirect:/manager/approvals";
    }
}
