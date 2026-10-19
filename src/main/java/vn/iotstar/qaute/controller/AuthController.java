package vn.iotstar.qaute.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.iotstar.qaute.dto.RegisterRequest;
import vn.iotstar.qaute.service.AuthService;

@RestController("registrationApiController")
@RequestMapping("/api/qaute/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        try {
            authService.register(request);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Registration submitted; verify the OTP sent to your email."));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
        @RequestParam String email,
        @RequestParam String code
    ) {
        try {
            authService.verifyRegistrationOtp(email, code);
            return ResponseEntity.ok(Map.of("message", "Email verified."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
        }
    }
}

