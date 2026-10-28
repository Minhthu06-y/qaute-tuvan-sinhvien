package vn.iotstar.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.function.Function;

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

    @Value("${spring.mail.username:}")
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
        sendOtp(email, "Xác minh tài khoản QAUTE", code ->
                "Mã OTP của bạn là: " + code
                + "\nMã có hiệu lực trong 5 phút."
                + "\nNếu bạn không thực hiện đăng ký, hãy bỏ qua email này.");
    }

    @Transactional
    public void sendPasswordResetOtp(String email) {
        sendOtp(email, "Đặt lại mật khẩu QAUTE", code ->
                "Mã OTP để đặt lại mật khẩu QAUTE của bạn là: " + code
                + "\nMã có hiệu lực trong 5 phút."
                + "\nNếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.");
    }

    private void sendOtp(String email, String subject, Function<String, String> messageText) {
        if (senderEmail == null || senderEmail.isBlank())
            throw new IllegalStateException("Chưa cấu hình dịch vụ email nên chưa thể gửi OTP. Vui lòng thử lại sau.");
        String normalizedEmail = email.trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        otpRepository.findTopByEmailOrderByIdDesc(normalizedEmail).ifPresent(latestOtp -> {
            LocalDateTime issuedAt = latestOtp.getExpiresAt().minusMinutes(5);
            if (issuedAt.plusSeconds(60).isAfter(now)) {
                throw new IllegalStateException("Vui lòng chờ một phút trước khi gửi lại mã OTP.");
            }
        });

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
        otp.setExpiresAt(now.plusMinutes(5));
        otp.setUsed(false);

        otpRepository.save(otp);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(normalizedEmail);
            message.setSubject(subject);
            message.setText(messageText.apply(code));
            mailSender.send(message);
        } catch (RuntimeException ex) {
            otp.setUsed(true);
            otpRepository.save(otp);
            throw new IllegalStateException("Không thể gửi mã xác minh. Vui lòng thử lại sau.");
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

        if (otp.getAttempts() >= 5) {
            otp.setUsed(true);
            otpRepository.save(otp);
            return false;
        }

        if (!passwordEncoder.matches(code, otp.getOtpHash())) {
            int attempts = otp.getAttempts() + 1;
            otp.setAttempts(attempts);
            if (attempts >= 5) otp.setUsed(true);
            otpRepository.save(otp);
            return false;
        }

        otp.setUsed(true);
        otpRepository.save(otp);
        return true;
    }
}
