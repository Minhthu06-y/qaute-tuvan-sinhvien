package vn.iotstar.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.entity.EmailOtp;
import vn.iotstar.repository.EmailOtpRepository;

@Service
public class OtpService {

    private final EmailOtpRepository otpRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username}")
    private String senderEmail;

    public OtpService(
            EmailOtpRepository otpRepository,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder) {
        this.otpRepository = otpRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void sendOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        // Vô hiệu hóa các OTP cũ chưa sử dụng.
        otpRepository
            .findTopByEmailAndUsedFalseOrderByIdDesc(normalizedEmail)
            .ifPresent(oldOtp -> {
                oldOtp.setUsed(true);
                otpRepository.save(oldOtp);
            });

        String code = String.valueOf(
            100000 + secureRandom.nextInt(900000)
        );

        EmailOtp otp = new EmailOtp();
        otp.setEmail(normalizedEmail);
        otp.setOtpHash(passwordEncoder.encode(code));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setUsed(false);

        otpRepository.save(otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(normalizedEmail);
        message.setSubject("Xác minh tài khoản QAUTE");
        message.setText(
            "Mã OTP của bạn là: " + code
            + "\nMã có hiệu lực trong 5 phút."
            + "\nNếu bạn không thực hiện đăng ký, hãy bỏ qua email này."
        );

        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            otp.setUsed(true);
            otpRepository.save(otp);
            throw new IllegalStateException(
                "Không thể gửi OTP. Vui lòng thử lại sau."
            );
        }
    }

    @Transactional
    public boolean verifyOtp(String email, String code) {
        String normalizedEmail = email.trim().toLowerCase();

        EmailOtp otp = otpRepository
            .findTopByEmailAndUsedFalseOrderByIdDesc(normalizedEmail)
            .orElse(null);

        if (otp == null) {
            return false;
        }

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otp.setUsed(true);
            otpRepository.save(otp);
            return false;
        }

        if (!passwordEncoder.matches(code, otp.getOtpHash())) {
            return false;
        }

        otp.setUsed(true);
        otpRepository.save(otp);
        return true;
    }
}