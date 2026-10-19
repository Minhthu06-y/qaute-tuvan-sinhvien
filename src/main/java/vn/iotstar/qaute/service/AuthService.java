package vn.iotstar.qaute.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.qaute.dto.RegisterRequest;
import vn.iotstar.qaute.entity.User;
import vn.iotstar.qaute.entity.Role;
import vn.iotstar.qaute.enums.UserStatus;
import vn.iotstar.qaute.repository.UserRepository;
import vn.iotstar.repository.UserAccountRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserAccountRepository legacyUsers;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final DeletedAccountEmailService deletedAccountEmails;

    @Value("${qaute.email-domain}")
    private String schoolEmailDomain;

    public AuthService(
            UserRepository userRepository,
            UserAccountRepository legacyUsers,
            PasswordEncoder passwordEncoder,
            OtpService otpService,
            DeletedAccountEmailService deletedAccountEmails) {
        this.userRepository = userRepository;
        this.legacyUsers = legacyUsers;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.deletedAccountEmails = deletedAccountEmails;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        deletedAccountEmails.releaseForReuse(email);

        if (!request.getPassword()
                .equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException(
                "Mật khẩu xác nhận không khớp."
            );
        }

        if (userRepository.existsByEmailIgnoreCase(email) || legacyUsers.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                "Email đã được đăng ký."
            );
        }

        if (request.isStudent()) {
            String requiredSuffix =
                "@" + schoolEmailDomain.trim().toLowerCase();

            if (!email.endsWith(requiredSuffix)) {
                throw new IllegalArgumentException(
                    "Sinh viên phải sử dụng email do trường cấp."
                );
            }

            if (request.isCurrentlyStudying()) {
                if (request.getStudentId() == null
                        || request.getStudentId().isBlank()) {
                    throw new IllegalArgumentException(
                        "Vui lòng nhập mã số sinh viên."
                    );
                }
            } else {
                LocalDate graduationDate = request.getGraduationDate();

                if (graduationDate == null
                        || graduationDate.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException(
                        "Vui lòng nhập ngày cấp bằng hợp lệ."
                    );
                }
            }
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(
            passwordEncoder.encode(request.getPassword())
        );
        user.setPhone(request.getPhone());
        user.setStudent(request.isStudent());
        user.setCurrentlyStudying(
            request.isStudent() && request.isCurrentlyStudying()
        );

        if (request.isStudent()) {
            user.setStudentId(request.getStudentId());

            if (!request.isCurrentlyStudying()) {
                user.setGraduationDate(request.getGraduationDate());
            }
        }

        // Người đăng ký mới chỉ là USER.
        // CONSULTANT, MANAGER, ADMIN được cấp quyền riêng.
        user.setRole(Role.USER);
        user.setStatus(UserStatus.INACTIVE);
        user.setEnabled(false);

        userRepository.saveAndFlush(user);

        otpService.sendOtp(email);
    }

    @Transactional
    public void verifyRegistrationOtp(String email, String code) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository
            .findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException(
                "Không tìm thấy tài khoản."
            ));

        if (user.isEnabled()) {
            throw new IllegalArgumentException(
                "Tài khoản đã được xác minh."
            );
        }

        if (!otpService.verifyOtp(normalizedEmail, code)) {
            throw new IllegalArgumentException(
                "OTP không chính xác hoặc đã hết hạn."
            );
        }

        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }
}
