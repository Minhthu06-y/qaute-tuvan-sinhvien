package vn.iotstar.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.iotstar.service.AdvisorPresenceService;
import vn.iotstar.service.SessionContext;

@RestController
public class PresenceController {
    private final SessionContext sessionContext;
    private final AdvisorPresenceService presence;

    public PresenceController(SessionContext sessionContext, AdvisorPresenceService presence) {
        this.sessionContext = sessionContext;
        this.presence = presence;
    }

    @PostMapping("/presence/heartbeat")
    public ResponseEntity<Void> heartbeat(HttpSession session) {
        if (session.getAttribute("userId") != null && sessionContext.viewer(session).isAdvisor()) {
            presence.heartbeat((Long) session.getAttribute("userId"), session.getId());
        }
        return ResponseEntity.noContent().build();
    }
}
