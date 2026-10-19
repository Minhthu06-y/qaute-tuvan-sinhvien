package vn.iotstar.qaute.config;

import vn.iotstar.qaute.entity.*;
import vn.iotstar.qaute.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.iotstar.qaute.enums.UserStatus;

/** Chỉ để chạy thử. Admin mặc định: admin@qaute.local / Admin@123 (ĐỔI trước khi nộp). */
@Component
@RequiredArgsConstructor
@Order(1)
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;

    @Override public void run(String... args) {
        if (departments.count() == 0)
            for (String n : new String[]{"Khoa Công Nghệ Thông Tin", "Khoa Cơ Khí Chế Tạo Máy", "Khoa Ngoại Ngữ",
                    "Khoa Công Nghệ May & Thời Trang", "Khoa Điện - Điện Tử", "Khoa Cơ Khí Động Lực", "Phòng Đào tạo",
                    "Khoa Xây Dựng", "Khoa Kinh Tế", "Khoa In - Truyền Thông", "Khoa Công Nghệ Hóa - Thực Phẩm",
                    "Khoa Đào Tạo Chất Lượng Cao", "Trung Tâm Dịch Vụ Sinh Viên - Tư vấn học đường",
                    "Phòng Tuyển sinh và Công Tác Sinh Viên - Chế độ chính sách", "Đoàn Thanh niên - Hội sinh viên",
                    "Khoa Khoa Học Ứng Dụng", "Viện Sư Phạm Kỹ Thuật", "Khoa Đào tạo Quốc Tế"})
                departments.save(new Department(n));
        if (!users.existsByEmailIgnoreCase("admin@qaute.local")) {
            User a = new User();
            a.setEmail("admin@qaute.local"); a.setFullName("Quản trị viên");
            a.setPasswordHash(encoder.encode("Admin@123")); a.setRole(Role.ADMIN);
            a.setStatus(UserStatus.ACTIVE); a.setEnabled(true);
            users.save(a);
        }
    }
}

