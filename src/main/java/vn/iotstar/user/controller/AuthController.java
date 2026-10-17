package vn.iotstar.user.controller;

import vn.iotstar.user.dto.RegisterRequest;
import vn.iotstar.user.dto.OtpVerifyRequest;
import vn.iotstar.user.service.RegistrationService;
import vn.iotstar.user.service.OtpService;
import vn.iotstar.user.repository.UserRepository;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final RegistrationService registrationService;
    private final OtpService otpService;
    private final UserRepository userRepository;

    public AuthController(
            RegistrationService registrationService,
            OtpService otpService,
            UserRepository userRepository) {
        this.registrationService = registrationService;
        this.otpService = otpService;
        this.userRepository = userRepository;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest")
            RegisterRequest request,
            BindingResult result,
            RedirectAttributes redirect) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch",
                    "Mật khẩu xác nhận không khớp");
            return "auth/register";
        }

        try {
            registrationService.register(request);
            redirect.addFlashAttribute("email", request.getEmail());
            redirect.addFlashAttribute("success",
                    "OTP đã được gửi đến email của bạn.");
            return "redirect:/verify-otp";
        } catch (IllegalArgumentException
                 | IllegalStateException ex) {
            result.reject("register.error", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/verify-otp")
    public String verifyPage(
            @RequestParam(required = false) String email,
            Model model) {
        if (email != null && !email.isBlank()) {
            model.addAttribute("email", email);
        }
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @Valid @ModelAttribute("otpRequest")
            OtpVerifyRequest request,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            return "auth/verify-otp";
        }

        try {
            registrationService.verifyOtp(
                request.getEmail(), request.getOtp());

            model.addAttribute("success",
                "Xác minh thành công. Bạn có thể đăng nhập.");

            return "auth/login";
        } catch (IllegalArgumentException
                 | IllegalStateException ex) {
            model.addAttribute("email", request.getEmail());
            model.addAttribute("error", ex.getMessage());
            return "auth/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    public String resendOtp(
            @RequestParam String email,
            RedirectAttributes redirect) {
        try {
            var user = userRepository.findByEmailIgnoreCase(email.trim());

            if (user.isEmpty() || user.get().isEnabled()) {
                redirect.addFlashAttribute("error",
                    "Không thể gửi OTP cho tài khoản này.");
                return "redirect:/verify-otp";
            }

            otpService.sendOtp(email);
            redirect.addFlashAttribute("success", "Đã gửi OTP mới.");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }

        redirect.addAttribute("email", email);
        return "redirect:/verify-otp";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
}