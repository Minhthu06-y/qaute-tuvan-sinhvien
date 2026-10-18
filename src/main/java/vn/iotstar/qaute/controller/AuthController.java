package vn.iotstar.qaute.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import vn.iotstar.qaute.dto.RegisterRequest;
import vn.iotstar.qaute.service.AuthService;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest")
            RegisterRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.register(request);
            return "redirect:/verify-otp?email="
                + java.net.URLEncoder.encode(
                    request.getEmail().trim(),
                    java.nio.charset.StandardCharsets.UTF_8
                );
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        } catch (IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtp(
            @RequestParam(required = false) String email,
            Model model) {

        model.addAttribute("email", email == null ? "" : email);
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam String email,
            @RequestParam String code,
            Model model) {

        try {
            authService.verifyRegistrationOtp(email, code);
            return "redirect:/login?verified";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("email", email);
            model.addAttribute("error", ex.getMessage());
            return "auth/verify-otp";
        }
    }

    @GetMapping("/login")
    public String showLogin() {
        return "auth/login";
    }
}