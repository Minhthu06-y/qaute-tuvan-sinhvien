package vn.iotstar.controller;

import java.util.Map;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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

@Controller
public class AuthController {
    private final UserAccountRepository users;
    private final FacultyRepository faculties;
    private final PasswordEncoder passwordEncoder;
    private final SessionContext sessionContext;
    private final JwtService jwtService;

    public AuthController(UserAccountRepository users, FacultyRepository faculties, PasswordEncoder passwordEncoder,
            SessionContext sessionContext, JwtService jwtService) {
        this.users = users; this.faculties = faculties; this.passwordEncoder = passwordEncoder; this.sessionContext = sessionContext; this.jwtService = jwtService;
    }

    @GetMapping("/login") public String loginPage() { return "login"; }
    @PostMapping("/login") public String login(@RequestParam String email, @RequestParam String password, HttpSession session, RedirectAttributes flash) {
        UserAccount user = users.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            flash.addFlashAttribute("error", "Email hoặc mật khẩu chưa chính xác."); return "redirect:/login";
        }
        sessionContext.login(session, user); return "redirect:/questions";
    }
    @PostMapping("/logout") public String logout(HttpSession session) { session.invalidate(); return "redirect:/login"; }

    @GetMapping("/register") public String registerPage(Model model) {
        model.addAttribute("faculties", faculties.findAll()); return "register";
    }
    @PostMapping("/register") public String register(@RequestParam String displayName, @RequestParam String email,
            @RequestParam String password, @RequestParam(required = false) Long facultyId, HttpSession session,
            RedirectAttributes flash, Model model) {
        String normalizedEmail = email.trim().toLowerCase();
        if (users.existsByEmail(normalizedEmail)) {
            model.addAttribute("error", "Email này đã được đăng ký."); model.addAttribute("faculties", faculties.findAll()); return "register";
        }
        if (password.length() < 6 || displayName.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập tên và mật khẩu có ít nhất 6 ký tự."); model.addAttribute("faculties", faculties.findAll()); return "register";
        }
        Faculty faculty = facultyId == null ? null : faculties.findById(facultyId).orElse(null);
        UserAccount user = users.save(new UserAccount(displayName.trim(), normalizedEmail, passwordEncoder.encode(password), "STUDENT", "Sinh viên", "", faculty));
        sessionContext.login(session, user); flash.addFlashAttribute("success", "Tạo tài khoản thành công. Bạn đang dùng chế độ sinh viên.");
        return "redirect:/questions";
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
            if (user == null || !encoder.matches(request.password(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Email hoặc mật khẩu chưa chính xác"));
            }
            return ResponseEntity.ok(new TokenResponse(jwtService.issue(user.getEmail(), user.getRoles()), "Bearer", user.getDisplayName(), user.getRoles()));
        }
        public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
        public record TokenResponse(String accessToken, String tokenType, String displayName, String roles) {}
    }
}
