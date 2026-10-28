
package vn.iotstar.service;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.dto.RegisterRequest;
import vn.iotstar.entity.User;
import vn.iotstar.enums.Role;
import vn.iotstar.enums.UserStatus;
import vn.iotstar.repository.UserRepository;
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

    /**
     * Kiểm tra email có thuộc tên miền email trường hay không.
     */
    private boolean isSchoolEmail(String email) {
        if (email == null || email.isBlank()
                || schoolEmailDomain == null
                || schoolEmailDomain.isBlank()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String requiredSuffix = "@"
                + schoolEmailDomain.trim().toLowerCase(Locale.ROOT);

        return normalizedEmail.endsWith(requiredSuffix);
    }

    /**
     * Đăng ký tài khoản và gửi OTP xác minh email.
     *
     * Quy tắc:
     * - Người không phải sinh viên: dùng email cá nhân.
     * - Sinh viên đang học: bắt buộc email trường và MSSV.
     * - Cựu sinh viên: được dùng email cá nhân hoặc email trường,
     *   bắt buộc MSSV và ngày cấp bằng.
     */
    @Transactional
    public void register(RegisterRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu đăng ký không hợp lệ.");
        }

        if (request.getEmail() == null
                || request.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập email.");
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Kiểm tra mật khẩu xác nhận.
        if (request.getPassword() == null
                || request.getConfirmPassword() == null
                || !request.getPassword()
                        .equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException(
                    "Mật khẩu xác nhận không khớp.");
        }

        // Kiểm tra tên người đăng ký.
        if (request.getFullName() == null
                || request.getFullName().isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập họ và tên.");
        }

        // Không cho đăng ký trùng email ở cả hai bảng tài khoản.
        if (userRepository.existsByEmailIgnoreCase(email)
                || legacyUsers.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "Email đã được đăng ký.");
        }

        boolean student = request.isStudent();
        boolean currentlyStudying =
                student && request.isCurrentlyStudying();

        // Kiểm tra thông tin sinh viên.
        if (student) {

            // Cả sinh viên đang học và cựu sinh viên đều cần MSSV.
            if (request.getStudentId() == null
                    || request.getStudentId().isBlank()) {
                throw new IllegalArgumentException(
                        "Vui lòng nhập mã số sinh viên.");
            }

            // Chỉ sinh viên đang học bắt buộc dùng email trường.
            if (currentlyStudying && !isSchoolEmail(email)) {
                throw new IllegalArgumentException(
                        "Sinh viên đang học phải sử dụng email "
                        + "trường có đuôi @student.hcmute.edu.vn.");
            }

            // Cựu sinh viên phải có ngày cấp bằng hợp lệ.
            if (!currentlyStudying) {
                LocalDate graduationDate =
                        request.getGraduationDate();

                if (graduationDate == null
                        || graduationDate.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException(
                            "Vui lòng nhập ngày cấp bằng hợp lệ, "
                            + "không được lớn hơn ngày hiện tại.");
                }
            }
        }

        // Tạo tài khoản mới.
        User user = new User();

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        user.setStudent(student);
        user.setCurrentlyStudying(currentlyStudying);

        if (student) {
            user.setStudentId(request.getStudentId().trim());

            if (!currentlyStudying) {
                user.setGraduationDate(request.getGraduationDate());
            } else {
                user.setGraduationDate(null);
            }
        } else {
            user.setStudentId(null);
            user.setGraduationDate(null);
        }

        // Người đăng ký mới chỉ có quyền USER.
        // CONSULTANT, MANAGER và ADMIN được cấp quyền riêng.
        user.setRole(Role.USER);
        user.setStatus(UserStatus.INACTIVE);
        user.setEnabled(false);

        // Lưu tài khoản chờ xác minh OTP.
        userRepository.saveAndFlush(user);

        // Gửi OTP đến email đăng ký.
        // Nếu gửi OTP thất bại và phát sinh RuntimeException,
        // giao dịch sẽ được rollback.
        otpService.sendOtp(email);
    }

    /**
     * Xác minh OTP và kích hoạt tài khoản trong bảng users.
     */
    @Transactional
    public void verifyRegistrationOtp(String email, String code) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email không hợp lệ.");
        }

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập mã OTP.");
        }

        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tài khoản."));

        if (user.isDeleted()) {
            throw new IllegalArgumentException(
                    "Tài khoản không tồn tại hoặc đã bị xóa.");
        }

        if (user.isEnabled()) {
            throw new IllegalArgumentException(
                    "Tài khoản đã được xác minh.");
        }

        if (!otpService.verifyOtp(normalizedEmail, code.trim())) {
            throw new IllegalArgumentException(
                    "OTP không chính xác hoặc đã hết hạn.");
        }

        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
    }
}
