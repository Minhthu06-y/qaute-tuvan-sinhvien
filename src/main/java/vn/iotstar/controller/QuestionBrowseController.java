package vn.iotstar.controller;

import vn.iotstar.entity.*;
import vn.iotstar.enums.*;
import vn.iotstar.repository.DepartmentRepository;
import vn.iotstar.service.*;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.Set;

/** Admin & Manager xem danh sách câu hỏi (chưa trả lời / đã trả lời / đã chuyển) và chi tiết. */
@Controller
@RequiredArgsConstructor
public class QuestionBrowseController {
    private final QuestionBrowseService service;
    private final DepartmentRepository departments;

    @GetMapping("/admin/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminList(@RequestParam(defaultValue = "UNANSWERED") QuestionStatus status,
                            @RequestParam(required = false) Long deptId, @RequestParam(required = false) String category,
                            @RequestParam(required = false) String kw,
                            @RequestParam(defaultValue = "0") int page, Model m) {
        return list("/admin", null, status, deptId, category, kw, page, m);
    }

    @GetMapping("/manager/questions")
    @PreAuthorize("hasRole('MANAGER')")
    public String managerList(@RequestParam(defaultValue = "UNANSWERED") QuestionStatus status,
                              @RequestParam(required = false) Long deptId, @RequestParam(required = false) String category,
                              @RequestParam(required = false) String kw,
                              @RequestParam(defaultValue = "0") int page, Model m) {
        return list("/manager", null, status, deptId, category, kw, page, m);
    }

    @GetMapping("/admin/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDetail(@PathVariable Long id, Model m) { return detail("/admin", null, id, m); }

    @GetMapping("/manager/questions/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public String managerDetail(@PathVariable Long id, Model m) {
        return detail("/manager", null, id, m);
    }

    @PostMapping("/manager/questions/{id}/answers")
    @PreAuthorize("hasRole('MANAGER')")
    public String answer(@PathVariable Long id, @RequestParam String content, Principal p, RedirectAttributes ra) {
        if (content == null || content.isBlank()) {
            ra.addFlashAttribute("err", "Nội dung trả lời không được để trống");
            return "redirect:/manager/questions/" + id;
        }
        service.answer(id, content, p.getName());
        ra.addFlashAttribute("msg", "Đã gửi câu trả lời cho sinh viên");
        return "redirect:/manager/questions/" + id;
    }

    private String list(String base, Set<Long> allowed, QuestionStatus status, Long deptId, String category,
                        String kw, int page, Model m) {
        m.addAttribute("base", base); m.addAttribute("status", status);
        m.addAttribute("deptId", deptId); m.addAttribute("category", category); m.addAttribute("kw", kw);
        m.addAttribute("categories", service.categories(allowed));
        m.addAttribute("page", service.search(status, deptId, category, kw, allowed, page, 15));
        m.addAttribute("counts", service.counts(allowed));
        m.addAttribute("departments", allowed == null ? departments.findAll() : departments.findAllById(allowed));
        return "questions/list";
    }

    private String detail(String base, Set<Long> allowed, Long id, Model m) {
        m.addAttribute("base", base);
        m.addAttribute("q", service.get(id, allowed));
        m.addAttribute("thread", service.thread(id));
        return "questions/detail";
    }
}

