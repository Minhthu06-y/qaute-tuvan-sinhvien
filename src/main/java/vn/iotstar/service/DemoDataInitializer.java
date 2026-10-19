package vn.iotstar.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.model.*;
import vn.iotstar.repository.*;

@Component
@Order(2)
public class DemoDataInitializer implements CommandLineRunner {
    private final FacultyRepository faculties;
    private final FacultyTopicRepository topics;
    private final UserAccountRepository users;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(FacultyRepository faculties, FacultyTopicRepository topics, UserAccountRepository users, QuestionRepository questions,
            AnswerRepository answers, PasswordEncoder passwordEncoder) {
        this.faculties = faculties; this.topics = topics; this.users = users; this.questions = questions; this.answers = answers; this.passwordEncoder = passwordEncoder;
    }

    @Override @Transactional
    public void run(String... args) {
        if (faculties.count() == 0) {
            List<String[]> names = List.of(
                new String[]{"Khoa Công Nghệ Thông Tin", "CNTT"}, new String[]{"Khoa Cơ Khí Chế Tạo Máy", "CKCTM"},
                new String[]{"Khoa Ngoại Ngữ", "NN"}, new String[]{"Khoa Công Nghệ May & Thời Trang", "CNM"},
                new String[]{"Khoa Điện - Điện Tử", "ĐĐT"}, new String[]{"Khoa Cơ Khí Động Lực", "CKĐL"},
                new String[]{"Phòng Đào tạo", "PĐT"}, new String[]{"Khoa Xây Dựng", "XD"}, new String[]{"Khoa Kinh Tế", "KT"},
                new String[]{"Khoa In - Truyền Thông", "ITTT"}, new String[]{"Khoa Công Nghệ Hóa - Thực Phẩm", "CNHTP"},
                new String[]{"Khoa Đào Tạo Chất Lượng Cao", "CLC"}, new String[]{"Trung Tâm Dịch Vụ Sinh Viên - Tư vấn học đường", "TTDVSV"},
                new String[]{"Phòng Tuyển sinh và Công Tác Sinh Viên - Chế độ chính sách", "TS-CTSV"},
                new String[]{"Đoàn Thanh niên - Hội sinh viên Trường Đại học Sư phạm Kỹ thuật Tp.HCM", "ĐTN"},
                new String[]{"Khoa Khoa Học Ứng Dụng", "KHƯD"}, new String[]{"Viện Sư Phạm Kỹ Thuật", "SPKT"},
                new String[]{"Khoa Đào tạo Quốc Tế", "ĐTQT"});
            names.forEach(row -> faculties.save(new Faculty(row[0], row[1])));
        }
        Faculty it = faculties.findAll().stream().filter(f -> "CNTT".equals(f.getShortName())).findFirst().orElse(faculties.findAll().getFirst());
        ensureFacultyTopics();
        UserAccount advisor = users.findByEmail("tuvanvien@qaute.edu.vn").orElseGet(() -> users.save(new UserAccount(
                "Nguyễn Minh Thư", "tuvanvien@qaute.edu.vn", passwordEncoder.encode("123456"), "STUDENT,ADVISOR", "Sinh viên kiêm Tư vấn viên khoa CNTT", "24133061", it)));
        UserAccount manager = users.findByEmail("manager@qaute.edu.vn").orElseGet(() -> users.save(new UserAccount(
                "Trần Minh Anh", "manager@qaute.edu.vn", passwordEncoder.encode("123456"), "MANAGER", "Trưởng chuyên viên tư vấn", "", it)));
        UserAccount student = users.findByEmail("sinhvien@qaute.edu.vn").orElseGet(() -> users.save(new UserAccount(
                "Lê Hoàng Nam", "sinhvien@qaute.edu.vn", passwordEncoder.encode("123456"), "STUDENT", "Sinh viên", "23110018", it)));
        UserAccount lecturer = users.findByEmail("giangvien@qaute.edu.vn").orElseGet(() -> users.save(new UserAccount(
                "Phạm Thu Hà", "giangvien@qaute.edu.vn", passwordEncoder.encode("123456"), "LECTURER", "Giảng viên khoa CNTT", "", it)));
        UserAccount staff = users.findByEmail("canbo@qaute.edu.vn").orElseGet(() -> users.save(new UserAccount(
                "Võ Quốc Bảo", "canbo@qaute.edu.vn", passwordEncoder.encode("123456"), "STAFF", "Cán bộ viên chức khoa CNTT", "", it)));
        advisor.setRoles("STUDENT,ADVISOR");
        advisor.setTitle("Sinh viên kiêm Tư vấn viên khoa CNTT · phụ trách Đồ án");
        manager.setRoles("MANAGER,ADVISOR");
        student.setRoles("STUDENT");
        lecturer.setRoles("LECTURER");
        staff.setRoles("STAFF");
        advisor.setAssignedTopics(Set.of(findTopic(it, "Chương trình đào tạo · Đồ án")));
        lecturer.setAssignedTopics(Set.of(findTopic(it, "Chương trình đào tạo · Ngành học")));
        staff.setAssignedTopics(Set.of(findTopic(it, "Chương trình đào tạo · Việc làm")));
        manager.setAssignedTopics(Set.of());
        student.setAssignedTopics(Set.of());
        users.saveAll(List.of(advisor, manager, student, lecturer, staff));
        if (questions.count() == 0) {
            LocalDateTime now = LocalDateTime.now();
            Question q1 = questions.save(new Question(student, it, "Em cần chuẩn bị gì để đăng ký học phần tự chọn?", "Em đang học năm hai ngành Công nghệ thông tin. Nhờ thầy cô hướng dẫn điều kiện tiên quyết và thời gian đăng ký học phần tự chọn ạ.", now.minusHours(3)));
            Question q2 = questions.save(new Question(advisor, it, "Xin tư vấn lộ trình học Java và Spring Boot", "Em muốn làm đồ án web bằng Spring Boot. Em nên học những nội dung nào trước và tìm tài liệu ở đâu?", now.minusDays(1)));
            Question q3 = questions.save(new Question(student, it, "Thủ tục xác nhận sinh viên", "Cho em hỏi cách xin giấy xác nhận sinh viên để bổ sung hồ sơ học bổng.", now.minusDays(2)));
            q3.setPublic(true); q3.setStatus(QuestionStatus.ANSWERED); q3.setReplyNotification(true); questions.save(q3);
            answers.save(new Answer(q2, advisor, "Em có thể học Java cơ bản, OOP, SQL rồi đến Spring MVC, JPA và REST API. Khoa có phòng thực hành mở vào chiều thứ Năm; em liên hệ văn phòng khoa để đăng ký.", now.minusHours(20), ApprovalStatus.PENDING));
            answers.save(new Answer(q3, advisor, "Em nộp yêu cầu trên cổng dịch vụ sinh viên, mục Giấy xác nhận. Nếu cần bản giấy, mang thẻ sinh viên đến Phòng Công tác sinh viên.", now.minusDays(1), ApprovalStatus.APPROVED));
            answers.save(new Answer(q3, manager, "Bổ sung: giấy xác nhận điện tử có mã QR được chấp nhận cho hồ sơ học bổng nội bộ.", now.minusHours(22), ApprovalStatus.APPROVED));
            questions.save(q1);
        }
        attachDefaultTopicsToExistingQuestions();
    }

    private void ensureFacultyTopics() {
        for (Faculty faculty : faculties.findAll()) {
            Set<String> existingNames = new HashSet<>();
            topics.findByFacultyIdOrderByIdAsc(faculty.getId()).forEach(topic -> existingNames.add(topic.getName()));
            for (String name : topicNames(faculty)) {
                if (existingNames.add(name)) {
                    topics.save(new FacultyTopic(faculty, name));
                }
            }
        }
    }

    private FacultyTopic findTopic(Faculty faculty, String name) {
        return topics.findByFacultyIdOrderByIdAsc(faculty.getId()).stream()
                .filter(topic -> topic.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing topic " + name + " for faculty " + faculty.getId()));
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

    private void attachDefaultTopicsToExistingQuestions() {
        for (Faculty faculty : faculties.findAll()) {
            List<String> allowedNames = topicNames(faculty);
            List<FacultyTopic> facultyTopics = topics.findByFacultyIdOrderByIdAsc(faculty.getId());
            for (Question question : questions.findByFacultyIdOrderByCreatedAtDesc(faculty.getId())) {
                if (question.getTopic() == null
                        || !question.getTopic().getFaculty().getId().equals(faculty.getId())
                        || !allowedNames.contains(question.getTopic().getName())) {
                    question.setTopic(topicForExistingQuestion(question, facultyTopics));
                    questions.save(question);
                }
            }
            questions.flush();
            topics.deleteAll(facultyTopics.stream().filter(topic -> !allowedNames.contains(topic.getName())).toList());
        }
    }

    private FacultyTopic topicForExistingQuestion(Question question, List<FacultyTopic> facultyTopics) {
        String text = (question.getSubject() + " " + question.getBody()).toLowerCase(Locale.ROOT);
        String preferred = switch (question.getFaculty().getShortName()) {
            case "PĐT" -> text.contains("đăng ký") ? "Đăng ký học phần" : text.contains("tốt nghiệp") ? "Tốt nghiệp" : text.contains("điểm") ? "Điểm" : "Học tập";
            case "TS-CTSV" -> text.contains("tuyển sinh") ? "Tuyển sinh" : text.contains("học bổng") ? "Học bổng" : text.contains("miễn giảm") ? "Miễn giảm" : text.contains("ký túc") ? "Ký túc xá" : "Rèn luyện";
            case "TTDVSV" -> text.contains("tâm lý") ? "Tâm lý" : text.contains("hướng nghiệp") ? "Hướng nghiệp" : text.contains("việc làm") ? "Việc làm" : "Kỹ năng";
            case "ĐTN" -> text.contains("đoàn") ? "Hoạt động Đoàn" : "Hoạt động Hội";
            case "CLC" -> "Chương trình chất lượng cao";
            case "ĐTQT" -> "Chương trình quốc tế";
            default -> text.contains("đồ án") ? "Đồ án" : text.contains("thực tập") ? "Thực tập" : text.contains("nghiên cứu") ? "Nghiên cứu" : text.contains("việc làm") ? "Việc làm" : "Ngành học";
        };
        return facultyTopics.stream().filter(topic -> topic.getName().endsWith(preferred)).findFirst()
                .orElseGet(() -> facultyTopics.stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("Missing topic for faculty " + question.getFaculty().getId())));
    }
}
