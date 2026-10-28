package vn.iotstar.config;

import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.iotstar.enums.*;

/** Seeds legacy department data and creates an admin only when credentials are supplied by configuration. */
@Component
@RequiredArgsConstructor
@Order(1)
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;

    @Value("${app.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @Override public void run(String... args) {
        if (departments.count() == 0)
            for (String n : new String[]{"Khoa Công Nghệ Thông Tin", "Khoa Cơ Khí Chế Tạo Máy", "Khoa Ngoại Ngữ",
                    "Khoa Công Nghệ May & Thời Trang", "Khoa Điện - Điện Tử", "Khoa Cơ Khí Động Lực", "Phòng Đào tạo",
                    "Khoa Xây Dựng", "Khoa Kinh Tế", "Khoa In - Truyền Thông", "Khoa Công Nghệ Hóa - Thực Phẩm",
                    "Khoa Đào Tạo Chất Lượng Cao", "Trung Tâm Dịch Vụ Sinh Viên - Tư vấn học đường",
                    "Phòng Tuyển sinh và Công Tác Sinh Viên - Chế độ chính sách", "Đoàn Thanh niên - Hội sinh viên",
                    "Khoa Khoa Học Ứng Dụng", "Viện Sư Phạm Kỹ Thuật", "Khoa Đào tạo Quốc Tế"})
                departments.save(new Department(n));
        if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()
                && !users.existsByEmailIgnoreCase(adminEmail.trim())) {
            User a = new User();
            a.setEmail(adminEmail.trim().toLowerCase(java.util.Locale.ROOT)); a.setFullName(DemoPersonNames.forRole(Role.ADMIN, 1));
            a.setPasswordHash(encoder.encode(adminPassword)); a.setRole(Role.ADMIN);
            a.setStatus(UserStatus.ACTIVE); a.setEnabled(true);
            users.save(a);
        }
    }
}

