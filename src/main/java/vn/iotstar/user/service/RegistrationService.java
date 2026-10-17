package vn.iotstar.user.service;

import vn.iotstar.user.dto.RegisterRequest;
import vn.iotstar.user.entity.Role;
import vn.iotstar.user.entity.User;
import vn.iotstar.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                "Email đã được đăng ký");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException(
                "Mật khẩu xác nhận không khớp");
        }

        if (request.getDateOfBirth() == null
                || !request.getDateOfBirth().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                "Ngày sinh không hợp lệ");
        }

        if (request.isUniversityStudent()) {
            if (!email.endsWith("@st.ute.udn.vn")
                    && !email.endsWith("@ute.udn.vn")) {
                throw new IllegalArgumentException(
                    "Vui lòng sử dụng email trường đã được cấp");
            }

            if (request.getStudentCode() == null
                    || request.getStudentCode().isBlank()) {
                throw new IllegalArgumentException(
                    "Vui lòng nhập mã số sinh viên");
            }

            if (request.getCurrentlyStudying() == null) {
                throw new IllegalArgumentException(
                    "Vui lòng xác nhận tình trạng học tập");
            }

            if (!request.getCurrentlyStudying()
                    && request.getGraduationDate() == null) {
                throw new IllegalArgumentException(
                    "Vui lòng nhập ngày cấp bằng");
            }
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone().trim());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setUniversityStudent(request.isUniversityStudent());

        if (request.isUniversityStudent()) {
            user.setStudentCode(request.getStudentCode().trim());
            user.setCurrentlyStudying(request.getCurrentlyStudying());
            user.setGraduationDate(request.getGraduationDate());
        } else {
            user.setStudentCode(null);
            user.setCurrentlyStudying(null);
            user.setGraduationDate(null);
        }

        user.setRole(Role.USER);
        user.setEnabled(false);
        user.setActive(true);

        userRepository.save(user);

        otpService.sendOtp(email);
    }

    @Transactional
    public void verifyOtp(String email, String code) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
            .orElseThrow(() -> new IllegalArgumentException(
                "Không tìm thấy tài khoản"));

        if (user.isEnabled()) {
            throw new IllegalStateException(
                "Tài khoản đã được xác minh");
        }

        if (!otpService.verify(email, code)) {
            throw new IllegalArgumentException(
                "OTP sai hoặc đã hết hạn");
        }

        user.setEnabled(true);
        userRepository.save(user);
    }
}