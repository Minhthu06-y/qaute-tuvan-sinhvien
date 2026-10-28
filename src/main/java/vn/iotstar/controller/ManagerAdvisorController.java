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
import vn.iotstar.service.AdvisorService;
import vn.iotstar.service.ScopeService;

@Controller
@RequestMapping("/manager/advisors")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerAdvisorController {
    private final AdvisorService advisors;
    private final ScopeService scope;
    private final vn.iotstar.repository.DepartmentRepository departments;

    @GetMapping
    public String page(Principal principal, Model model) {
        var allowed = scope.managerDepartments(principal.getName());
        model.addAttribute("advisors", advisors.inDepartments(allowed));
        model.addAttribute("departments", departments.findAllById(allowed));
        return "manager/advisors";
    }

    @PostMapping("/{id}/department")
    public String assign(@PathVariable Long id, @RequestParam Long departmentId,
            Principal principal, RedirectAttributes flash) {
        advisors.assignDepartment(id, departmentId, principal.getName(),
                scope.managerDepartments(principal.getName()));
        flash.addFlashAttribute("msg", "Đã cập nhật khoa và chuyên đề được phân công cho tư vấn viên.");
        return "redirect:/manager/advisors";
    }
}
