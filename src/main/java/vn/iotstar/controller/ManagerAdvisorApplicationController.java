package vn.iotstar.controller;

import java.security.Principal;

import jakarta.validation.constraints.NotBlank;
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
import vn.iotstar.service.AdvisorApplicationService;
import vn.iotstar.service.FieldException;

@Controller
@RequestMapping("/manager/advisor-applications")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerAdvisorApplicationController {
    private final AdvisorApplicationService applications;

    @GetMapping
    public String page(Principal principal, Model model) {
        model.addAttribute("applications", applications.pendingForManager(principal.getName()));
        return "manager/advisor-applications";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        try {
            applications.approveByManager(id, principal.getName());
            flash.addFlashAttribute("msg", "Đã duyệt hồ sơ và chuyển admin cấp quyền tư vấn viên.");
        } catch (FieldException error) {
            flash.addFlashAttribute("err", error.getMessage());
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("err", error.getMessage());
        }
        return "redirect:/manager/advisor-applications";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam @NotBlank String reviewNote,
            Principal principal, RedirectAttributes flash) {
        try {
            applications.rejectByManager(id, principal.getName(), reviewNote);
            flash.addFlashAttribute("msg", "Đã từ chối hồ sơ và gửi kết quả cho người đăng ký.");
        } catch (FieldException error) {
            flash.addFlashAttribute("err", error.getMessage());
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("err", error.getMessage());
        }
        return "redirect:/manager/advisor-applications";
    }
}
