package vn.iotstar.qaute.controller;

import vn.iotstar.qaute.entity.QuestionStatus;
import vn.iotstar.qaute.repository.DepartmentRepository;
import vn.iotstar.qaute.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Set;

/** Admin & Manager xem danh sách câu hỏi (chưa trả lời / đã trả lời / đã chuyển) và chi tiết. */
@Controller
@RequiredArgsConstructor
public class QuestionBrowseController {
    private final QuestionBrowseService service;
    private final DepartmentRepository departments;
    private final ScopeService scope;

    @GetMapping("/admin/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminList(@RequestParam(defaultValue = "UNANSWERED") QuestionStatus status,
                            @RequestParam(required = false) Long deptId, @RequestParam(required = false) String kw,
                            @RequestParam(defaultValue = "0") int page, Model m) {
        return list("/admin", null, status, deptId, kw, page, m);
    }

    @GetMapping("/manager/questions")
    @PreAuthorize("hasRole('MANAGER')")
    public String managerList(@RequestParam(defaultValue = "UNANSWERED") QuestionStatus status,
                              @RequestParam(required = false) Long deptId, @RequestParam(required = false) String kw,
                              @RequestParam(defaultValue = "0") int page, Principal p, Model m) {
        return list("/manager", scope.managerDepartments(p.getName()), status, deptId, kw, page, m);
    }

    @GetMapping("/admin/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDetail(@PathVariable Long id, Model m) { return detail("/admin", null, id, m); }

    @GetMapping("/manager/questions/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public String managerDetail(@PathVariable Long id, Principal p, Model m) {
        return detail("/manager", scope.managerDepartments(p.getName()), id, m);
    }

    private String list(String base, Set<Long> allowed, QuestionStatus status, Long deptId, String kw, int page, Model m) {
        m.addAttribute("base", base); m.addAttribute("status", status);
        m.addAttribute("deptId", deptId); m.addAttribute("kw", kw);
        m.addAttribute("page", service.search(status, deptId, kw, allowed, page, 15));
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

