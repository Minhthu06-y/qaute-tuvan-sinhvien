package vn.iotstar.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.AdvisorApplicationForm;
import vn.iotstar.enums.AdvisorType;
import vn.iotstar.model.FacultyTopic;
import vn.iotstar.repository.DepartmentRepository;
import vn.iotstar.repository.FacultyTopicRepository;
import vn.iotstar.service.AdvisorApplicationService;
import vn.iotstar.service.FieldException;
import vn.iotstar.service.SessionContext;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'CONSULTANT')")
public class AdvisorApplicationController {
    private final AdvisorApplicationService applications;
    private final DepartmentRepository departments;
    private final FacultyTopicRepository topics;
    private final SessionContext sessionContext;

    @GetMapping("/advisor-application")
    public String page(HttpSession session, Model model) {
        var viewer = sessionContext.viewer(session);
        if (viewer == null || !viewer.isStudent())
            return viewer != null && viewer.isAdvisor() ? "redirect:/account" : "redirect:/questions";
        if (!model.containsAttribute("form")) model.addAttribute("form", new AdvisorApplicationForm());
        model.addAttribute("latestApplication", applications.latest(viewer.user().getId()));
        return formModel(model);
    }

    @PostMapping("/advisor-application")
    public String submit(@Valid @ModelAttribute("form") AdvisorApplicationForm form, BindingResult binding,
            HttpSession session, Model model, RedirectAttributes flash) {
        var viewer = sessionContext.viewer(session);
        if (viewer == null || !viewer.isStudent())
            return viewer != null && viewer.isAdvisor() ? "redirect:/account" : "redirect:/questions";
        if (!binding.hasErrors()) {
            try {
                applications.submit(viewer.user(), form);
                flash.addFlashAttribute("success", "Đăng ký thành công. Yêu cầu đang chờ manager hoặc admin duyệt; quyền tư vấn viên sẽ được cấp sau khi duyệt.");
                return "redirect:/advisor-application";
            } catch (FieldException error) {
                binding.rejectValue(error.getField(), "custom", error.getMessage());
            }
        }
        model.addAttribute("latestApplication", applications.latest(viewer.user().getId()));
        return formModel(model);
    }

    private String formModel(Model model) {
        var departments = departmentsRepository();
        var topics = topicsRepository();
        model.addAttribute("departments", departments);
        model.addAttribute("topics", topics);
        model.addAttribute("advisorTypes", java.util.List.of(
                AdvisorType.STUDENT, AdvisorType.ADVISOR, AdvisorType.LECTURER, AdvisorType.STAFF));
        model.addAttribute("audiences", vn.iotstar.enums.AudienceType.values());
        return "advisor-application";
    }

    private java.util.List<vn.iotstar.entity.Department> departmentsRepository() { return departments.findAll(); }
    private java.util.List<FacultyTopic> topicsRepository() { return topics.findAllByOrderByFacultyIdAscIdAsc(); }
}
