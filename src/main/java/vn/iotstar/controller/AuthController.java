package vn.iotstar.controller;

import java.util.Map;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.iotstar.model.AccountRole;
import vn.iotstar.model.Faculty;
import vn.iotstar.model.UserAccount;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.UserAccountRepository;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.SessionContext;
import vn.iotstar.service.AdvisorPresenceService;
import vn.iotstar.qaute.entity.Role;
import vn.iotstar.qaute.entity.User;
import vn.iotstar.qaute.service.OtpService;
import vn.iotstar.qaute.service.DeletedAccountEmailService;
import vn.iotstar.qaute.enums.UserStatus;
import vn.iotstar.qaute.repository.UserRepository;

@Controller
public class AuthController {
    private final UserAccountRepository users;
    private final FacultyRepository faculties;
    private final PasswordEncoder passwordEncoder;
    private final SessionContext sessionContext;
    private final JwtService jwtService;
    private final AdvisorPresenceService advisorPresence;
    private final UserRepository qauteUsers;
    private final OtpService otpService;
    private final DeletedAccountEmailService deletedAccountEmails;

    public AuthController(UserAccountRepository users, FacultyRepository faculties, PasswordEncoder passwordEncoder,
            SessionContext sessionContext, JwtService jwtService, AdvisorPresenceService advisorPresence,
            UserRepository qauteUsers, OtpService otpService, DeletedAccountEmailService deletedAccountEmails) {
        this.users = users; this.faculties = faculties; this.passwordEncoder = passwordEncoder;
        this.sessionContext = sessionContext; this.jwtService = jwtService; this.advisorPresence = advisorPresence;
        this.qauteUsers = qauteUsers; this.otpService = otpService;
        this.deletedAccountEmails = deletedAccountEmails;
    }

    @GetMapping("/login") public String loginPage() { return "login"; }
    @PostMapping("/login") public String login(@RequestParam String email, @RequestParam String password,
            HttpSession session, HttpServletRequest request, RedirectAttributes flash) {
        String normalizedEmail = email.trim().toLowerCase();
        User qauteUser = qauteUsers.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (qauteUser != null && (qauteUser.getRole() == Role.ADMIN || qauteUser.getRole() == Role.MANAGER)) {
            if (qauteUser.isEnabled() && !qauteUser.isDeleted() && qauteUser.getStatus() == UserStatus.ACTIVE
                    && passwordEncoder.matches(password, qauteUser.getPasswordHash())) {
                request.changeSessionId();
                session.removeAttribute("userId");
                session.removeAttribute("activeRole");
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(qauteUser.getEmail(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + qauteUser.getRole().name()))));
                SecurityContextHolder.setContext(context);
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
                if (qauteUser.getRole() == Role.MANAGER) {
                    users.findByEmailIgnoreCase(normalizedEmail)
                            .filter(account -> account.isEnabled() && !account.isDeleted())
                            .ifPresent(legacyManager -> sessionContext.login(session, legacyManager));
                }
                return "redirect:/" + qauteUser.getRole().name().toLowerCase() + "/dashboard";
            }
            flash.addFlashAttribute("error", "Email hoặc mật khẩu chưa chính xác.");
            return "redirect:/login";
        }
        UserAccount user = users.findByEmail(normalizedEmail).orElse(null);
        if (user != null && user.isEnabled() && !user.isDeleted()
                && passwordEncoder.matches(password, user.getPasswordHash())) {
            SecurityContextHolder.clearContext();
            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            sessionContext.login(session, user);
            return "redirect:/questions";
        }

        flash.addFlashAttribute("error", "Email hoặc mật khẩu chưa chính xác.");
        return "redirect:/login";
    }
    @PostMapping("/logout") public String logout(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id && sessionContext.viewer(session).isAdvisor()) advisorPresence.offline(id, session.getId());
        SecurityContextHolder.clearContext();
        session.invalidate(); return "redirect:/login";
    }

    @GetMapping("/register") public String registerPage(Model model) {
        model.addAttribute("faculties", faculties.findAll()); return "register";
    }
    @PostMapping("/register") public String register(@RequestParam String displayName, @RequestParam String email,
            @RequestParam String password, @RequestParam(required = false) Long facultyId, HttpSession session,
            RedirectAttributes flash, Model model) {
        String normalizedEmail = email.trim().toLowerCase();
        deletedAccountEmails.releaseForReuse(normalizedEmail);
        if (users.existsByEmailIgnoreCase(normalizedEmail) || qauteUsers.existsByEmailIgnoreCase(normalizedEmail)) {
            model.addAttribute("error", "Email này đã được đăng ký."); model.addAttribute("faculties", faculties.findAll()); return "register";
        }
        if (password.length() < 6 || displayName.isBlank() || !normalizedEmail.contains("@")) {
            model.addAttribute("error", "Vui lòng nhập tên, email hợp lệ và mật khẩu có ít nhất 6 ký tự."); model.addAttribute("faculties", faculties.findAll()); return "register";
        }
        Faculty faculty = facultyId == null ? null : faculties.findById(facultyId).orElse(null);
        UserAccount user = new UserAccount(displayName.trim(), normalizedEmail, passwordEncoder.encode(password), "STUDENT", "Sinh viên", "", faculty);
        user.setEnabled(false);
        users.save(user);
        try {
            otpService.sendOtp(normalizedEmail);
            session.setAttribute("pendingRegistrationEmail", normalizedEmail);
            flash.addFlashAttribute("success", "Mã xác minh đã được gửi đến email của bạn.");
            return "redirect:/register/verify";
        } catch (IllegalStateException ex) {
            users.delete(user);
            model.addAttribute("error", ex.getMessage()); model.addAttribute("faculties", faculties.findAll());
            return "register";
        }
    }

    @GetMapping("/register/verify") public String verifyRegistrationPage(HttpSession session, Model model, RedirectAttributes flash) {
        String email = (String) session.getAttribute("pendingRegistrationEmail");
        UserAccount user = email == null ? null : users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.isEnabled() || user.isDeleted()) {
            session.removeAttribute("pendingRegistrationEmail");
            flash.addFlashAttribute("error", "Không có đăng ký nào đang chờ xác minh.");
            return "redirect:/register";
        }
        model.addAttribute("email", email);
        return "register-verify";
    }

    @PostMapping("/register/verify") public String verifyRegistration(@RequestParam String code, HttpSession session,
            RedirectAttributes flash) {
        String email = (String) session.getAttribute("pendingRegistrationEmail");
        UserAccount user = email == null ? null : users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.isEnabled() || user.isDeleted()) {
            session.removeAttribute("pendingRegistrationEmail");
            flash.addFlashAttribute("error", "Không có đăng ký nào đang chờ xác minh.");
            return "redirect:/register";
        }
        if (!otpService.verifyOtp(email, code)) {
            flash.addFlashAttribute("error", "Mã OTP không chính xác hoặc đã hết hạn.");
            return "redirect:/register/verify";
        }
        user.setEnabled(true);
        users.save(user);
        session.removeAttribute("pendingRegistrationEmail");
        SecurityContextHolder.clearContext();
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        sessionContext.login(session, user);
        flash.addFlashAttribute("success", "Email đã được xác minh. Tài khoản của bạn đã sẵn sàng.");
        return "redirect:/questions";
    }

    @PostMapping("/register/resend-otp") public String resendRegistrationOtp(HttpSession session, RedirectAttributes flash) {
        String email = (String) session.getAttribute("pendingRegistrationEmail");
        UserAccount user = email == null ? null : users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.isEnabled() || user.isDeleted()) {
            session.removeAttribute("pendingRegistrationEmail");
            flash.addFlashAttribute("error", "Không có đăng ký nào đang chờ xác minh.");
            return "redirect:/register";
        }
        try {
            otpService.sendOtp(email);
            flash.addFlashAttribute("success", "Mã OTP mới đã được gửi đến email của bạn.");
        } catch (IllegalStateException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/register/verify";
    }

    @GetMapping("/demo/switch") public String switchDemo(@RequestParam(defaultValue = "advisor") String role, HttpSession session) {
        AccountRole selected = switch (role.toLowerCase()) {
            case "student" -> AccountRole.STUDENT; case "lecturer" -> AccountRole.LECTURER;
            case "staff" -> AccountRole.STAFF; case "manager" -> AccountRole.MANAGER; default -> AccountRole.ADVISOR;
        };
        sessionContext.switchDemo(session, selected); return "redirect:/questions";
    }
    @GetMapping("/demo/role") public String switchRole(@RequestParam String role, HttpSession session, RedirectAttributes flash) {
        try { sessionContext.switchRole(session, AccountRole.valueOf(role.toUpperCase())); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", "Vai trò không hợp lệ."); }
        return "redirect:/questions";
    }

    @RestController
    @RequestMapping("/api/auth")
    @Validated
    public static class TokenController {
        private final UserAccountRepository users;
        private final PasswordEncoder encoder;
        private final JwtService jwtService;
        public TokenController(UserAccountRepository users, PasswordEncoder encoder, JwtService jwtService) {
            this.users = users; this.encoder = encoder; this.jwtService = jwtService;
        }
        @PostMapping("/token") public ResponseEntity<?> token(@RequestBody LoginRequest request) {
            UserAccount user = users.findByEmail(request.email().trim().toLowerCase()).orElse(null);
            if (user == null || !user.isEnabled() || !encoder.matches(request.password(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Email hoặc mật khẩu chưa chính xác"));
            }
            return ResponseEntity.ok(new TokenResponse(jwtService.issue(user.getEmail(), user.getRoles()), "Bearer", user.getDisplayName(), user.getRoles()));
        }
        public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
        public record TokenResponse(String accessToken, String tokenType, String displayName, String roles) {}
    }
}
