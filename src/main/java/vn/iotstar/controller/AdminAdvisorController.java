package vn.iotstar.controller;

import vn.iotstar.dto.AdvisorForm;
import vn.iotstar.entity.*;
import vn.iotstar.enums.*;
import vn.iotstar.repository.DepartmentRepository;
import vn.iotstar.service.*;

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
@RequestMapping("/admin/advisors")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAdvisorController {
    private final AdvisorService service;
    private final DepartmentRepository departments;

    @GetMapping
    public String list(@RequestParam(required = false) String kw, @RequestParam(required = false) Long deptId,
                       @RequestParam(required = false) AdvisorStatus status, Model m) {
        m.addAttribute("advisors", service.search(kw, deptId, status));
        m.addAttribute("departments", departments.findAll());
        m.addAttribute("kw", kw); m.addAttribute("deptId", deptId); m.addAttribute("status", status);
        m.addAttribute("statuses", AdvisorStatus.values());
        return "admin/advisors";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        AdvisorForm f = new AdvisorForm();
        f.setStatus(AdvisorStatus.ACTIVE);
        f.setAdvisorType(AdvisorType.ADVISOR);
        return form(m, f, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AdvisorForm f, BindingResult br,
                         Model m, Principal p, RedirectAttributes ra) {
        if (!br.hasErrors()) {
            try {
                service.create(f, p.getName());
                ra.addFlashAttribute("msg", "Đã thêm tư vấn viên " + f.getEmail());
                return "redirect:/admin/advisors";
            } catch (FieldException e) { br.rejectValue(e.getField(), "custom", e.getMessage()); }
        }
        clearInvalid(f, br);
        m.addAttribute("showEmailModal", br.hasFieldErrors("email"));
        return form(m, f, null);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model m) {
        AdvisorProfile a = service.get(id);
        AdvisorForm f = new AdvisorForm();
        f.setFullName(a.getUser().getFullName()); f.setEmail(a.getUser().getEmail());
        f.setPhone(a.getUser().getPhone()); f.setDepartmentId(a.getDepartment().getId());
        f.setTopicIds(a.getUser().getAssignedTopics().stream().map(topic -> topic.getId())
                .collect(java.util.stream.Collectors.toSet()));
        f.setAdvisorType(a.getAdvisorType() == null ? AdvisorType.ADVISOR : a.getAdvisorType());
        f.setAudience(a.getAudience());
        f.setStatus(a.getStatus()); f.setHead(a.isHead());
        return form(m, f, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") AdvisorForm f, BindingResult br,
                         Model m, Principal p, RedirectAttributes ra) {
        // email không đổi khi sửa -> bỏ lỗi email nếu có
        f.setEmail(service.get(id).getUser().getEmail());
        if (!br.hasErrors() || onlyEmailError(br)) {
            try {
                service.update(id, f, p.getName());
                ra.addFlashAttribute("msg", "Đã cập nhật thông tin tư vấn viên");
                return "redirect:/admin/advisors";
            } catch (FieldException e) { br.rejectValue(e.getField(), "custom", e.getMessage()); }
        }
        clearInvalid(f, br);
        return form(m, f, id);
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        AdvisorStatus s = service.toggleStatus(id, p.getName());
        ra.addFlashAttribute("msg", s == AdvisorStatus.ACTIVE ? "Đã kích hoạt lại tư vấn viên"
                : "Đã ngừng hoạt động: tư vấn viên mất quyền tư vấn, chỉ còn quyền hỏi");
        return "redirect:/admin/advisors";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        service.delete(id, p.getName());
        ra.addFlashAttribute("msg", "Đã gỡ khỏi đội ngũ tư vấn (giữ nguyên câu hỏi, câu trả lời và lịch sử hoạt động)");
        return "redirect:/admin/advisors";
    }

    private String form(Model m, AdvisorForm f, Long editId) {
        m.addAttribute("form", f); m.addAttribute("editId", editId);
        m.addAttribute("departments", departments.findAll());
        m.addAttribute("topics", service.topicCatalog());
        m.addAttribute("audiences", AudienceType.values());
        m.addAttribute("advisorTypes", AdvisorType.values());
        m.addAttribute("statuses", AdvisorStatus.values());
        return "admin/advisor-form";
    }

    private boolean onlyEmailError(BindingResult br) {
        return br.getFieldErrors().stream().allMatch(e -> e.getField().equals("email"));
    }

    /** Giữ lại dữ liệu đúng, chỉ xoá ô bị lỗi (template dùng th:value thay vì th:field để không bị rejectedValue ghi đè). */
    private void clearInvalid(AdvisorForm f, BindingResult br) {
        br.getFieldErrors().forEach(e -> {
            switch (e.getField()) {
                case "fullName" -> f.setFullName(null);
                case "email" -> f.setEmail(null);
                case "phone" -> f.setPhone(null);
                case "departmentId" -> f.setDepartmentId(null);
                case "audience" -> f.setAudience(null);
                case "status" -> f.setStatus(null);
            }
        });
    }
}

