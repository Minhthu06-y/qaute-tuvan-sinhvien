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
@RequestMapping("/admin/advisor-applications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAdvisorApplicationController {
    private final AdvisorApplicationService applications;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("applications", applications.approvedByManagersForAdmin());
        return "admin/advisor-applications";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        try {
            applications.grantByAdmin(id, principal.getName());
            flash.addFlashAttribute("msg", "Đã cấp quyền tư vấn viên cho tài khoản.");
        } catch (FieldException error) {
            flash.addFlashAttribute("err", error.getMessage());
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("err", error.getMessage());
        }
        return "redirect:/admin/advisor-applications";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam @NotBlank String reviewNote,
            Principal principal, RedirectAttributes flash) {
        try {
            applications.rejectByAdmin(id, principal.getName(), reviewNote);
            flash.addFlashAttribute("msg", "Đã từ chối cấp quyền và gửi kết quả cho người đăng ký.");
        } catch (FieldException error) {
            flash.addFlashAttribute("err", error.getMessage());
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("err", error.getMessage());
        }
        return "redirect:/admin/advisor-applications";
    }
}
