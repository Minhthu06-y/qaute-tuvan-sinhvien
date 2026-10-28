package vn.iotstar.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.iotstar.entity.User;
import vn.iotstar.enums.Role;
import vn.iotstar.enums.UserStatus;
import vn.iotstar.model.Faculty;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.security.JwtService;
import vn.iotstar.service.AdvisorPresenceService;
import vn.iotstar.service.DeletedAccountEmailService;
import vn.iotstar.service.OtpService;
import vn.iotstar.service.SessionContext;

@Controller
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository users;
    private final FacultyRepository faculties;
    private final PasswordEncoder passwordEncoder;
    private final SessionContext sessionContext;
    private final JwtService jwtService;
    private final AdvisorPresenceService advisorPresence;
    private final OtpService otpService;
    private final DeletedAccountEmailService deletedAccountEmails;

    public AuthController(UserRepository users, FacultyRepository faculties, PasswordEncoder passwordEncoder,
            SessionContext sessionContext, JwtService jwtService, AdvisorPresenceService advisorPresence,
            OtpService otpService, DeletedAccountEmailService deletedAccountEmails) {
        this.users = users;
        this.faculties = faculties;
        this.passwordEncoder = passwordEncoder;
        this.sessionContext = sessionContext;
        this.jwtService = jwtService;
        this.advisorPresence = advisorPresence;
        this.otpService = otpService;
        this.deletedAccountEmails = deletedAccountEmails;
    }

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(@RequestParam String email, HttpSession session, RedirectAttributes flash) {
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        session.setAttribute("pendingPasswordResetEmail", normalizedEmail);

        User user = users.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user != null && user.isEnabled() && user.getStatus() == UserStatus.ACTIVE && !user.isDeleted()) {
            try {
                otpService.sendPasswordResetOtp(normalizedEmail);
            } catch (IllegalStateException ex) {
                // Keep the response the same whether the address exists or email delivery fails.
                log.warn("Could not send a password-reset OTP email: {}", ex.getMessage());
            }
        }

        flash.addFlashAttribute("success",
                "Nếu email thuộc một tài khoản đang hoạt động, mã đặt lại mật khẩu sẽ được gửi đến email đó.");
        return "redirect:/reset-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(HttpSession session, Model model, RedirectAttributes flash) {
        String email = (String) session.getAttribute("pendingPasswordResetEmail");
        if (email == null || email.isBlank()) {
            flash.addFlashAttribute("error", "Vui lòng nhập email để nhận mã đặt lại mật khẩu.");
            return "redirect:/forgot-password";
        }
        model.addAttribute("email", email);
        return "reset-password";
    }

    @PostMapping("/reset-password/resend-otp")
    public String resendPasswordResetOtp(HttpSession session, RedirectAttributes flash) {
        String email = (String) session.getAttribute("pendingPasswordResetEmail");
        User user = email == null ? null : users.findByEmailIgnoreCase(email).orElse(null);
        if (user != null && user.isEnabled() && user.getStatus() == UserStatus.ACTIVE && !user.isDeleted()) {
            try {
                otpService.sendPasswordResetOtp(email);
            } catch (IllegalStateException ex) {
                // Use a neutral response to avoid confirming whether the account exists.
                log.warn("Could not resend a password-reset OTP email: {}", ex.getMessage());
            }
        }
        flash.addFlashAttribute("success",
                "Nếu email thuộc một tài khoản đang hoạt động, mã đặt lại mật khẩu sẽ được gửi đến email đó.");
        return "redirect:/reset-password";
    }

    @PostMapping("/reset-password")
    @Transactional
    public String resetPassword(@RequestParam String code, @RequestParam String newPassword,
            @RequestParam String confirmPassword, HttpSession session, RedirectAttributes flash) {
        String email = (String) session.getAttribute("pendingPasswordResetEmail");
        if (email == null || email.isBlank()) {
            flash.addFlashAttribute("error", "Phiên đặt lại mật khẩu đã hết hạn. Vui lòng bắt đầu lại.");
            return "redirect:/forgot-password";
        }
        if (newPassword.length() < 8
                || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            flash.addFlashAttribute("error", "Mật khẩu mới cần có ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
            return "redirect:/reset-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            flash.addFlashAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "redirect:/reset-password";
        }

        User user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isEnabled() || user.getStatus() != UserStatus.ACTIVE || user.isDeleted()
                || !otpService.verifyOtp(email, code)) {
            flash.addFlashAttribute("error", "Mã xác minh không chính xác hoặc đã hết hạn.");
            return "redirect:/reset-password";
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        SecurityContextHolder.clearContext();
        session.invalidate();
        flash.addFlashAttribute("success", "Mật khẩu đã được cập nhật. Hãy đăng nhập bằng mật khẩu mới.");
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
            HttpSession session, HttpServletRequest request, RedirectAttributes flash) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = users.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user != null && user.isEnabled() && user.getStatus() == UserStatus.ACTIVE
                && passwordEncoder.matches(password, user.getPasswordHash())) {
            establishLogin(request, session, user);
            if (user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER) {
                return "redirect:/" + user.getRole().name().toLowerCase() + "/dashboard";
            }
            return "redirect:/questions";
        }

        flash.addFlashAttribute("error", "Email hoặc mật khẩu chưa chính xác.");
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        Object userId = session.getAttribute("userId");
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (userId instanceof Long id && viewer != null && viewer.isAdvisor()) {
            advisorPresence.offline(id, session.getId());
        }
        SecurityContextHolder.clearContext();
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("faculties", faculties.findAll());
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String displayName, @RequestParam String email,
            @RequestParam String password, @RequestParam(required = false) Long facultyId, HttpSession session,
            RedirectAttributes flash, Model model) {
        String normalizedEmail = email.trim().toLowerCase();
        deletedAccountEmails.releaseForReuse(normalizedEmail);
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            model.addAttribute("error", "Email này đã được đăng ký.");
            model.addAttribute("faculties", faculties.findAll());
            return "register";
        }
        if (password.length() < 6 || displayName.isBlank() || !normalizedEmail.contains("@")) {
            model.addAttribute("error", "Vui lòng nhập tên, email hợp lệ và mật khẩu có ít nhất 6 ký tự.");
            model.addAttribute("faculties", faculties.findAll());
            return "register";
        }

        Faculty faculty = facultyId == null ? null : faculties.findById(facultyId).orElse(null);
        User user = new User(displayName.trim(), normalizedEmail, passwordEncoder.encode(password),
                "STUDENT", "Sinh viên", "", faculty);
        user.setEnabled(false);
        user.setStatus(UserStatus.INACTIVE);
        users.save(user);
        try {
            otpService.sendOtp(normalizedEmail);
            session.setAttribute("pendingRegistrationEmail", normalizedEmail);
            flash.addFlashAttribute("success", "Mã xác minh đã được gửi đến email của bạn.");
            return "redirect:/register/verify";
        } catch (IllegalStateException ex) {
            users.delete(user);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("faculties", faculties.findAll());
            return "register";
        }
    }

    @GetMapping("/register/verify")
    public String verifyRegistrationPage(HttpSession session, Model model, RedirectAttributes flash) {
        String email = (String) session.getAttribute("pendingRegistrationEmail");
        User user = email == null ? null : users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.isEnabled() || user.isDeleted()) {
            session.removeAttribute("pendingRegistrationEmail");
            flash.addFlashAttribute("error", "Không có đăng ký nào đang chờ xác minh.");
            return "redirect:/register";
        }
        model.addAttribute("email", email);
        return "register-verify";
    }

    @PostMapping("/register/verify")
    public String verifyRegistration(@RequestParam String code, HttpSession session, HttpServletRequest request,
            RedirectAttributes flash) {
        String email = (String) session.getAttribute("pendingRegistrationEmail");
        User user = email == null ? null : users.findByEmailIgnoreCase(email).orElse(null);
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
        user.setStatus(UserStatus.ACTIVE);
        users.save(user);
        session.removeAttribute("pendingRegistrationEmail");
        establishLogin(request, session, user);
        flash.addFlashAttribute("success", "Email đã được xác minh. Tài khoản của bạn đã sẵn sàng.");
        return "redirect:/questions";
    }

    @PostMapping("/register/resend-otp")
    public String resendRegistrationOtp(HttpSession session, RedirectAttributes flash) {
        String email = (String) session.getAttribute("pendingRegistrationEmail");
        User user = email == null ? null : users.findByEmailIgnoreCase(email).orElse(null);
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

    private void establishLogin(HttpServletRequest request, HttpSession session, User user) {
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        sessionContext.login(session, user);
    }

    @RestController
    @RequestMapping("/api/auth")
    @Validated
    public static class TokenController {
        private final UserRepository users;
        private final PasswordEncoder encoder;
        private final JwtService jwtService;

        public TokenController(UserRepository users, PasswordEncoder encoder, JwtService jwtService) {
            this.users = users;
            this.encoder = encoder;
            this.jwtService = jwtService;
        }

        @PostMapping("/token")
        public ResponseEntity<?> token(@RequestBody LoginRequest request) {
            User user = users.findByEmailIgnoreCase(request.email().trim().toLowerCase()).orElse(null);
            if (user == null || !user.isEnabled() || user.getRole() == null || user.getStatus() != UserStatus.ACTIVE
                    || !encoder.matches(request.password(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Email hoặc mật khẩu chưa chính xác"));
            }
            String role = user.getRole().name();
            return ResponseEntity.ok(new TokenResponse(jwtService.issue(user.getEmail(), role),
                    "Bearer", user.getDisplayName(), role));
        }

        public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
        public record TokenResponse(String accessToken, String tokenType, String displayName, String role) {}
    }
}
