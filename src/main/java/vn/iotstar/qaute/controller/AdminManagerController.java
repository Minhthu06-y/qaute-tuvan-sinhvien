package vn.iotstar.qaute.controller;

import vn.iotstar.qaute.dto.ManagerForm;
import vn.iotstar.qaute.repository.DepartmentRepository;
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
@RequestMapping("/admin/managers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminManagerController {
    private final ManagerAccountService service;
    private final DepartmentRepository departments;

    @GetMapping
    public String page(Model m) {
        if (!m.containsAttribute("form")) m.addAttribute("form", new ManagerForm());
        m.addAttribute("managers", service.list());
        m.addAttribute("departments", departments.findAll());
        return "admin/managers";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ManagerForm f, BindingResult br,
                         Model m, Principal p, RedirectAttributes ra) {
        if (!br.hasErrors()) {
            try {
                service.create(f, p.getName());
                ra.addFlashAttribute("msg", "Đã tạo tài khoản Manager " + f.getEmail());
                return "redirect:/admin/managers";
            } catch (FieldException e) { br.rejectValue(e.getField(), "custom", e.getMessage()); }
        }
        f.setPassword(null);
        return page(m);
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        boolean on = service.toggleEnabled(id, p.getName());
        ra.addFlashAttribute("msg", on ? "Đã mở khoá tài khoản" : "Đã khoá tài khoản");
        return "redirect:/admin/managers";
    }
}

