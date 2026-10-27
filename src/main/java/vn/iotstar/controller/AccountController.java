package vn.iotstar.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import vn.iotstar.enums.AccountRole;
import vn.iotstar.model.*;

import vn.iotstar.service.SessionContext;

@Controller
public class AccountController {
    private final SessionContext sessionContext;

    public AccountController(SessionContext sessionContext) { this.sessionContext = sessionContext; }

    @GetMapping("/account")
    public String account(HttpSession session, Model model) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        List<Map<String, Object>> roles = Arrays.stream(viewer.user().getRoles().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    AccountRole role = AccountRole.valueOf(value);
                    return Map.<String, Object>of("code", role.name(), "label", roleLabel(role), "active", role == viewer.role());
                }).toList();
        model.addAttribute("accountNumber", String.format("QA-%06d", viewer.user().getId()));
        model.addAttribute("accountRoles", roles);
        model.addAttribute("activeRoleLabel", roleLabel(viewer.role()));
        String assignedTopics = viewer.user().getAssignedTopics().stream()
                .map(topic -> topic.getName()).sorted().collect(java.util.stream.Collectors.joining(", "));
        if (assignedTopics.isBlank()) {
            if (viewer.user().hasRole(AccountRole.MANAGER)) assignedTopics = "Toàn bộ chuyên đề trong khoa";
            else if (viewer.user().hasRole(AccountRole.ADVISOR) || viewer.user().hasRole(AccountRole.LECTURER)
                    || viewer.user().hasRole(AccountRole.STAFF)) assignedTopics = "Chưa được phân công";
            else assignedTopics = "Không áp dụng";
        }
        model.addAttribute("assignedTopicsLabel", assignedTopics);
        return "account";
    }

    private String roleLabel(AccountRole role) {
        return switch (role) {
            case STUDENT -> "Sinh viên";
            case ADVISOR -> "Tư vấn viên";
            case LECTURER -> "Giảng viên";
            case STAFF -> "Cán bộ viên chức";
            case MANAGER -> "Trưởng ban cố vấn";
        };
    }
}
