package vn.iotstar.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.iotstar.service.SessionContext;

@ControllerAdvice
public class SharedModelAdvice {
    private final SessionContext sessionContext;
    public SharedModelAdvice(SessionContext sessionContext) { this.sessionContext = sessionContext; }
    @ModelAttribute public void shared(Model model, HttpSession session) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (viewer == null) {
            model.addAttribute("currentUser", null);
            model.addAttribute("currentRole", null);
            model.addAttribute("currentRoleLabel", "");
            model.addAttribute("isStudent", false);
            model.addAttribute("isAdvisor", false);
            model.addAttribute("isManager", false);
            model.addAttribute("isAdmin", false);
            model.addAttribute("notificationUserId", null);
            return;
        }
        model.addAttribute("currentUser", viewer.user());
        model.addAttribute("currentRole", viewer.role().name());
        model.addAttribute("currentRoleLabel", switch (viewer.role()) {
            case USER -> "Người dùng";
            case CONSULTANT -> "Tư vấn viên";
            case MANAGER -> "Quản lý khoa";
            case ADMIN -> "Quản trị viên";
        });
        model.addAttribute("isStudent", viewer.isStudent());
        model.addAttribute("isAdvisor", viewer.isAdvisor());
        model.addAttribute("isManager", viewer.isManager());
        model.addAttribute("isAdmin", viewer.role() == vn.iotstar.enums.Role.ADMIN);
        model.addAttribute("notificationUserId", viewer.user().getId());
    }
}



