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
        model.addAttribute("currentUser", viewer.user());
        model.addAttribute("activeRole", viewer.role().name());
        model.addAttribute("isStudent", viewer.isStudent());
        model.addAttribute("isAdvisor", viewer.isAdvisor());
        model.addAttribute("isManager", viewer.isManager());
        model.addAttribute("notificationUserId", viewer.user().getId());
    }
}



