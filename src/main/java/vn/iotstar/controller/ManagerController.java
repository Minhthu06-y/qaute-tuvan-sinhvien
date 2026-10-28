package vn.iotstar.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.service.SessionContext;

@Controller
public class ManagerController {
    private final SessionContext sessionContext;

    public ManagerController(SessionContext sessionContext) {
        this.sessionContext = sessionContext;
    }

    /** Keep the legacy URL, but route every manager through the single review workflow. */
    @GetMapping("/manager/approvals")
    public String approvals(HttpSession session) {
        if (!sessionContext.viewer(session).isManager()) return "redirect:/questions";
        return "redirect:/manager/reviews?status=PENDING";
    }

    /** Old approval forms now open the canonical review page instead of writing a separate review path. */
    @PostMapping("/manager/answers/{id}/review")
    public String review(@PathVariable Long id,
            HttpSession session, RedirectAttributes flash) {
        if (!sessionContext.viewer(session).isManager())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        flash.addFlashAttribute("msg", "Vui lòng hoàn tất quyết định trên biểu mẫu duyệt thống nhất.");
        return "redirect:/manager/reviews/" + id;
    }
}
