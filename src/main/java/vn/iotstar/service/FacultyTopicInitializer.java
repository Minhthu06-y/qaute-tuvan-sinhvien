package vn.iotstar.service;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.model.Faculty;
import vn.iotstar.model.FacultyTopic;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.FacultyTopicRepository;

/** Creates the faculty and topic catalog when the database does not contain it yet. */
@Component
@Order(2)
public class FacultyTopicInitializer implements CommandLineRunner {
    private final FacultyRepository faculties;
    private final FacultyTopicRepository topics;

    public FacultyTopicInitializer(FacultyRepository faculties, FacultyTopicRepository topics) {
        this.faculties = faculties;
        this.topics = topics;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (faculties.count() == 0) {
            List<String[]> names = List.of(
                    new String[]{"Khoa Công Nghệ Thông Tin", "CNTT"},
                    new String[]{"Khoa Cơ Khí Chế Tạo Máy", "CKCTM"},
                    new String[]{"Khoa Ngoại Ngữ", "NN"},
                    new String[]{"Khoa Công Nghệ May & Thời Trang", "CNM"},
                    new String[]{"Khoa Điện - Điện Tử", "ĐĐT"},
                    new String[]{"Khoa Cơ Khí Động Lực", "CKĐL"},
                    new String[]{"Phòng Đào tạo", "PĐT"},
                    new String[]{"Khoa Xây Dựng", "XD"},
                    new String[]{"Khoa Kinh Tế", "KT"},
                    new String[]{"Khoa In - Truyền Thông", "ITTT"},
                    new String[]{"Khoa Công Nghệ Hóa - Thực Phẩm", "CNHTP"},
                    new String[]{"Khoa Đào Tạo Chất Lượng Cao", "CLC"},
                    new String[]{"Trung Tâm Dịch Vụ Sinh Viên - Tư vấn học đường", "TTDVSV"},
                    new String[]{"Phòng Tuyển sinh và Công Tác Sinh Viên - Chế độ chính sách", "TS-CTSV"},
                    new String[]{"Đoàn Thanh niên - Hội sinh viên Trường Đại học Sư phạm Kỹ thuật Tp.HCM", "ĐTN"},
                    new String[]{"Khoa Khoa Học Ứng Dụng", "KHƯD"},
                    new String[]{"Viện Sư Phạm Kỹ Thuật", "SPKT"},
                    new String[]{"Khoa Đào tạo Quốc Tế", "ĐTQT"});
            names.forEach(row -> faculties.save(new Faculty(row[0], row[1])));
        }

        for (Faculty faculty : faculties.findAll()) {
            for (String name : topicNames(faculty)) {
                boolean exists = topics.findByFacultyIdOrderByIdAsc(faculty.getId()).stream()
                        .anyMatch(topic -> topic.getName().equals(name));
                if (!exists) topics.saveAndFlush(new FacultyTopic(faculty, name));
            }
        }
    }

    private List<String> topicNames(Faculty faculty) {
        return switch (faculty.getShortName()) {
            case "PĐT" -> List.of("Học vụ · Học tập", "Học vụ · Điểm", "Học vụ · Đăng ký học phần", "Học vụ · Tốt nghiệp");
            case "TS-CTSV" -> List.of("Tuyển sinh", "Chế độ chính sách · Học bổng", "Chế độ chính sách · Miễn giảm",
                    "Công tác sinh viên · Ký túc xá", "Công tác sinh viên · Rèn luyện");
            case "TTDVSV" -> List.of("Tư vấn học đường · Tâm lý", "Tư vấn học đường · Hướng nghiệp",
                    "Tư vấn học đường · Việc làm", "Tư vấn học đường · Kỹ năng");
            case "ĐTN" -> List.of("Hoạt động Đoàn", "Hoạt động Hội");
            case "CLC" -> List.of("Chương trình chất lượng cao");
            case "ĐTQT" -> List.of("Chương trình quốc tế");
            default -> List.of("Chương trình đào tạo · Ngành học", "Chương trình đào tạo · Đồ án",
                    "Chương trình đào tạo · Thực tập", "Chương trình đào tạo · Nghiên cứu", "Chương trình đào tạo · Việc làm");
        };
    }
}
