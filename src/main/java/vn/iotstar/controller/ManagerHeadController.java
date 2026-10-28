package vn.iotstar.controller;

import vn.iotstar.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

/** Manager quản lý "trưởng chuyên viên tư vấn" - CHỈ trong các khoa mình phụ trách (kiểm tra ở tầng service). */
@Controller
@RequestMapping("/manager/heads")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerHeadController {
    private final AdvisorService advisors;
    private final ScopeService scope;

    @GetMapping
    public String page(Principal p, Model m) {
        m.addAttribute("base", "/manager");
        m.addAttribute("advisors", advisors.inDepartments(scope.managerDepartments(p.getName())));
        return "manager/heads";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        try {
            boolean now = advisors.toggleHead(id, p.getName(), "MANAGER", scope.managerDepartments(p.getName()));
            ra.addFlashAttribute("msg", now ? "Đã chỉ định đầu mối tư vấn" : "Đã gỡ phân công đầu mối tư vấn");
        } catch (IllegalArgumentException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/manager/heads";
    }
}

