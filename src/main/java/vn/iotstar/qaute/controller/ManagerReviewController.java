package vn.iotstar.qaute.controller;

import vn.iotstar.qaute.dto.ReviewForm;
import vn.iotstar.qaute.entity.*;
import vn.iotstar.qaute.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

@Controller
@RequestMapping("/manager")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerReviewController {
    private final ReviewService service;

    @GetMapping("/reviews")
    public String list(@RequestParam(defaultValue = "PENDING") ApprovalStatus status, Principal p, Model m) {
        m.addAttribute("answers", service.listByStatus(status, p.getName()));
        m.addAttribute("status", status);
        return "manager/reviews";
    }

    @GetMapping("/reviews/{id}")
    public String detail(@PathVariable Long id, Principal p, Model m) {
        if (!m.containsAttribute("form")) m.addAttribute("form", new ReviewForm());
        return detailModel(id, p, m);
    }

    @PostMapping("/reviews/{id}")
    public String decide(@PathVariable Long id, @Valid @ModelAttribute("form") ReviewForm f, BindingResult br,
                         Model m, Principal p, RedirectAttributes ra) {
        if (!br.hasErrors()) {
            try {
                service.decide(id, f, p.getName());
                ra.addFlashAttribute("msg", f.getDecision() == ReviewDecision.APPROVED
                        ? "Đã duyệt câu trả lời" : "Đã từ chối và gửi câu trả lời đề xuất");
                return "redirect:/manager/reviews";
            } catch (FieldException e) { br.rejectValue(e.getField(), "custom", e.getMessage()); }
        }
        return detailModel(id, p, m);
    }

    @PostMapping("/answers/{id}/rate")
    public String rate(@PathVariable Long id, @RequestParam int rating, @RequestParam(required = false) String comment,
                       Principal p, RedirectAttributes ra) {
        service.rate(id, rating, comment, p.getName());
        ra.addFlashAttribute("msg", "Đã lưu đánh giá hiệu quả");
        return "redirect:/manager/reviews/" + id;
    }

    private String detailModel(Long id, Principal p, Model m) {
        Answer a = service.getForManager(id, p.getName());   // 403 nếu khác khoa
        m.addAttribute("answer", a); m.addAttribute("thread", service.thread(a));
        m.addAttribute("history", service.history(id));
        return "manager/review-detail";
    }
}

