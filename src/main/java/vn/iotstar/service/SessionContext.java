package vn.iotstar.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import vn.iotstar.enums.AccountRole;
import vn.iotstar.model.UserAccount;
import vn.iotstar.repository.UserAccountRepository;

@Service
public class SessionContext {
    private final UserAccountRepository users;
    public SessionContext(UserAccountRepository users) { this.users = users; }

    public Viewer viewer(HttpSession session) {
        Long id = (Long) session.getAttribute("userId");
        UserAccount user = id == null ? users.findByEmail("tuvanvien@qaute.edu.vn").orElseThrow() : users.findById(id).orElseGet(() -> users.findByEmail("tuvanvien@qaute.edu.vn").orElseThrow());
        String active = (String) session.getAttribute("activeRole");
        AccountRole role;
        try { role = active == null ? defaultRole(user) : AccountRole.valueOf(active); }
        catch (IllegalArgumentException ex) { role = defaultRole(user); }
        if (!user.hasRole(role)) role = defaultRole(user);
        return new Viewer(user, role);
    }
    public void login(HttpSession session, UserAccount user) {
        session.setAttribute("userId", user.getId());
        session.setAttribute("activeRole", defaultRole(user).name());
    }
    public void switchDemo(HttpSession session, AccountRole role) {
        String email = switch (role) {
            case STUDENT -> "sinhvien@qaute.edu.vn";
            case ADVISOR -> "tuvanvien@qaute.edu.vn";
            case LECTURER -> "giangvien@qaute.edu.vn";
            case STAFF -> "canbo@qaute.edu.vn";
            case MANAGER -> "manager@qaute.edu.vn";
        };
        UserAccount user = users.findByEmail(email).orElseThrow();
        session.setAttribute("userId", user.getId()); session.setAttribute("activeRole", role.name());
    }
    public void switchRole(HttpSession session, AccountRole role) { session.setAttribute("activeRole", role.name()); }
    private AccountRole defaultRole(UserAccount user) {
        if (user.hasRole(AccountRole.MANAGER)) return AccountRole.MANAGER;
        if (user.hasRole(AccountRole.ADVISOR)) return AccountRole.ADVISOR;
        if (user.hasRole(AccountRole.LECTURER)) return AccountRole.LECTURER;
        if (user.hasRole(AccountRole.STAFF)) return AccountRole.STAFF;
        return AccountRole.STUDENT;
    }
    public record Viewer(UserAccount user, AccountRole role) {
        public boolean isStudent() { return role == AccountRole.STUDENT; }
        public boolean isAdvisor() { return role == AccountRole.ADVISOR || role == AccountRole.LECTURER || role == AccountRole.STAFF; }
        public boolean isManager() { return role == AccountRole.MANAGER; }
    }
}
