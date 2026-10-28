package vn.iotstar.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import vn.iotstar.enums.Role;
import vn.iotstar.service.SessionContext;

@Controller
public class AccountController {
    private final SessionContext sessionContext;

    public AccountController(SessionContext sessionContext) { this.sessionContext = sessionContext; }

    @GetMapping("/account")
    public String account(HttpSession session, Model model) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        Role role = viewer.role();
        model.addAttribute("accountNumber", String.format("QA-%06d", viewer.user().getId()));
        model.addAttribute("accountRoles", List.of(Map.of(
                "code", role.name(), "label", roleLabel(role), "active", true)));
        model.addAttribute("currentRoleLabel", roleLabel(role));

        String assignedTopics = viewer.user().getAssignedTopics().stream()
                .map(topic -> topic.getName()).sorted().collect(java.util.stream.Collectors.joining(", "));
        if (assignedTopics.isBlank()) {
            if (viewer.isManager()) assignedTopics = "Toàn bộ chuyên đề trong khoa";
            else if (role == Role.CONSULTANT) assignedTopics = "Chưa được phân công";
            else assignedTopics = "Không áp dụng";
        }
        model.addAttribute("assignedTopicsLabel", assignedTopics);
        return "account";
    }

    private String roleLabel(Role role) {
        return switch (role) {
            case USER -> "Người dùng";
            case CONSULTANT -> "Tư vấn viên";
            case MANAGER -> "Quản lý khoa";
            case ADMIN -> "Quản trị viên";
        };
    }
}
