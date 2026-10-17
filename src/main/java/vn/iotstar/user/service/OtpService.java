package vn.iotstar.user.service;

import vn.iotstar.user.entity.OtpVerification;
import vn.iotstar.user.repository.OtpVerificationRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private final OtpVerificationRepository repository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder encoder;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.otp.expiration-minutes:5}")
    private int expirationMinutes;

    @Value("${app.otp.resend-seconds:60}")
    private int resendSeconds;

    private final SecureRandom random = new SecureRandom();

    public OtpService(
            OtpVerificationRepository repository,
            JavaMailSender mailSender,
            PasswordEncoder encoder) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.encoder = encoder;
    }

    public void sendOtp(String email) {
        String normalized = email.trim().toLowerCase();

        var existing = repository.findByEmailIgnoreCase(normalized);
        LocalDateTime now = LocalDateTime.now();

        if (existing.isPresent()) {
            var otp = existing.get();

            if (otp.getLastSentAt().plusSeconds(resendSeconds).isAfter(now)) {
                throw new IllegalStateException(
                    "Vui lòng đợi trước khi gửi OTP mới");
            }

            repository.delete(otp);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));

        OtpVerification verification = new OtpVerification(
            normalized,
            encoder.encode(code),
            now.plusMinutes(expirationMinutes),
            now
        );

        repository.save(verification);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(normalized);
        message.setSubject("Mã xác thực tài khoản QAUTE");
        message.setText(
            "Mã OTP của bạn là: " + code
            + "\nMã có hiệu lực trong " + expirationMinutes
            + " phút. Không chia sẻ mã này cho người khác."
        );

        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            repository.delete(verification);
            throw new IllegalStateException(
                "Không gửi được email OTP. Vui lòng thử lại.");
        }
    }

    public boolean verify(String email, String code) {
        var optional = repository.findByEmailIgnoreCase(
            email.trim().toLowerCase());

        if (optional.isEmpty()) return false;

        var otp = optional.get();

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())
                || otp.getAttempts() >= 5) {
            repository.delete(otp);
            return false;
        }

        if (!encoder.matches(code, otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            repository.save(otp);
            return false;
        }

        repository.delete(otp);
        return true;
    }
}