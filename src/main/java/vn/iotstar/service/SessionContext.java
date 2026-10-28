package vn.iotstar.service;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.iotstar.entity.User;
import vn.iotstar.enums.AdvisorStatus;
import vn.iotstar.enums.Role;
import vn.iotstar.repository.AdvisorProfileRepository;
import vn.iotstar.repository.UserRepository;

@Service
public class SessionContext {
    private final UserRepository users;
    private final AdvisorProfileRepository advisors;

    public SessionContext(UserRepository users, AdvisorProfileRepository advisors) {
        this.users = users;
        this.advisors = advisors;
    }

    public Viewer viewer(HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> authenticatedUser = Optional.empty();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            authenticatedUser = users.findByEmailIgnoreCase(authentication.getName());
        }
        User user = authenticatedUser.orElse(null);
        if (user == null || !user.isEnabled() || user.getStatus() != vn.iotstar.enums.UserStatus.ACTIVE) return null;
        boolean activeAdvisor = user.getRole() == Role.CONSULTANT
                && advisors.existsByUserIdAndStatusAndDeletedFalse(user.getId(), AdvisorStatus.ACTIVE);
        return new Viewer(user, user.getRole(), activeAdvisor);
    }

    public void login(HttpSession session, User user) {
        session.setAttribute("userId", user.getId());
    }

    public record Viewer(User user, Role role, boolean activeAdvisor) {
        public boolean isStudent() { return role == Role.USER; }
        public boolean isAdvisor() { return role == Role.CONSULTANT && activeAdvisor; }
        public boolean isManager() { return role == Role.MANAGER; }
    }
}
