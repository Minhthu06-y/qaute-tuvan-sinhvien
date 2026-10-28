package vn.iotstar.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.iotstar.entity.AdvisorProfile;
import vn.iotstar.entity.Department;
import vn.iotstar.entity.User;
import vn.iotstar.enums.AdvisorStatus;
import vn.iotstar.enums.AdvisorType;
import vn.iotstar.enums.ApprovalStatus;
import vn.iotstar.enums.AudienceType;
import vn.iotstar.enums.QuestionStatus;
import vn.iotstar.enums.Role;
import vn.iotstar.enums.UserStatus;
import vn.iotstar.model.Answer;
import vn.iotstar.model.Faculty;
import vn.iotstar.model.FacultyTopic;
import vn.iotstar.model.Question;
import vn.iotstar.repository.AdvisorProfileRepository;
import vn.iotstar.repository.AnswerRepository;
import vn.iotstar.repository.DepartmentRepository;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.FacultyTopicRepository;
import vn.iotstar.repository.QuestionRepository;
import vn.iotstar.repository.UserRepository;

/** Adds repeatable local QAUTE accounts and sample questions/answers for demonstrations. */
@Component
@Profile("local")
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class DemoContentSeeder implements CommandLineRunner {
    private static final int USER_COUNT = 12;
    private static final int CONSULTANT_COUNT = 6;
    private static final int MANAGER_COUNT = 4;
    private static final String EMAIL_DOMAIN = "@qaute.edu.vn";

    private static final List<String> QUESTION_SUBJECTS = List.of(
            "Không đăng ký được học phần kỳ này",
            "Cách xem điểm tổng kết học kỳ",
            "Điều kiện xét học bổng khuyến khích",
            "Thủ tục xin giấy xác nhận sinh viên",
            "Tài khoản cổng sinh viên bị khóa",
            "Cách đăng ký học cải thiện",
            "Thời hạn nộp hồ sơ tốt nghiệp",
            "Xin tư vấn chọn chuyên ngành phù hợp",
            "Lịch thi cuối kỳ được công bố ở đâu",
            "Thủ tục đăng ký thực tập",
            "Trường có chương trình trao đổi quốc tế không",
            "Em cần hỗ trợ tâm lý học đường",
            "Cách tham gia hoạt động Đoàn và Hội",
            "Thủ tục xin miễn giảm học phí",
            "Tra cứu kết quả đăng ký ký túc xá",
            "Quy định đánh giá điểm rèn luyện",
            "Điều kiện đăng ký đồ án tốt nghiệp",
            "Xin cấp lại thẻ sinh viên",
            "Thời gian phản hồi hồ sơ tuyển sinh",
            "Các chứng chỉ ngoại ngữ đầu ra được chấp nhận",
            "Cách đăng ký học lại môn còn nợ",
            "Quy trình phúc khảo điểm thi",
            "Thủ tục chuyển lớp học phần",
            "Nơi nhận tư vấn việc làm cho sinh viên");

    private final UserRepository users;
    private final DepartmentRepository departments;
    private final FacultyRepository faculties;
    private final FacultyTopicRepository facultyTopics;
    private final AdvisorProfileRepository advisorProfiles;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.demo-account-password:123456}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(String... args) {
        List<Department> departmentList = departments.findAll().stream()
                .sorted((left, right) -> left.getId().compareTo(right.getId())).toList();
        List<Faculty> facultyList = faculties.findAll();
        if (departmentList.isEmpty() || facultyList.isEmpty()) {
            log.warn("Skipped QAUTE demo content: faculty or department catalog is empty");
            return;
        }

        Map<Long, Faculty> facultyByDepartment = new HashMap<>();
        Map<Long, List<FacultyTopic>> topicsByFaculty = new HashMap<>();
        for (Department department : departmentList) {
            Faculty faculty = findFaculty(department, facultyList);
            if (faculty != null) {
                facultyByDepartment.put(department.getId(), faculty);
                topicsByFaculty.put(faculty.getId(), facultyTopics.findByFacultyIdOrderByIdAsc(faculty.getId()));
            }
        }

        List<User> demoUsers = new ArrayList<>();
        for (int i = 1; i <= USER_COUNT; i++) {
            demoUsers.add(account("user", i, Role.USER, null, null));
        }

        List<User> demoConsultants = new ArrayList<>();
        for (int i = 1; i <= CONSULTANT_COUNT; i++) {
            Department department = departmentList.get((i - 1) % departmentList.size());
            Faculty faculty = facultyByDepartment.get(department.getId());
            User consultant = account("consultant", i, Role.CONSULTANT, department, faculty);
            consultant.setTitle("T\u01B0 v\u1EA5n vi\u00EAn");
            // Hibernate may merge this entity and mutate the collection while syncing it.
            consultant.setConsultantTitles(new HashSet<>(Set.of(AdvisorType.ADVISOR)));
            List<FacultyTopic> assignedTopics = faculty == null ? List.of()
                    : topicsByFaculty.getOrDefault(faculty.getId(), List.of());
            consultant.setAssignedTopics(new HashSet<>(assignedTopics));
            consultant = users.save(consultant);

            AdvisorProfile profile = advisorProfiles.findByUserId(consultant.getId()).orElseGet(AdvisorProfile::new);
            profile.setUser(consultant);
            profile.setDepartment(department);
            profile.setAdvisorType(AdvisorType.ADVISOR);
            profile.setAudience(AudienceType.SINH_VIEN);
            profile.setStatus(AdvisorStatus.ACTIVE);
            profile.setDeleted(false);
            profile.setDeletedAt(null);
            profile.setDeletedBy(null);
            advisorProfiles.save(profile);
            demoConsultants.add(consultant);
        }

        List<User> demoManagers = new ArrayList<>();
        for (int i = 1; i <= MANAGER_COUNT; i++) {
            Department department = departmentList.get((i + 6) % departmentList.size());
            Faculty faculty = facultyByDepartment.get(department.getId());
            User manager = account("manager", i, Role.MANAGER, department, faculty);
            manager.setTitle("Manager");
            manager.getManagedDepartments().add(department);
            for (int offset = 0; offset < 3; offset++) {
                manager.getManagedDepartments().add(departmentList.get((i + 6 + offset * MANAGER_COUNT) % departmentList.size()));
            }
            demoManagers.add(users.save(manager));
        }

        createQuestionsAndAnswers(departmentList, facultyByDepartment, topicsByFaculty,
                demoUsers, demoConsultants, demoManagers);
        log.info("Local QAUTE demo data is ready: {} users, {} consultants, {} managers; sample questions and answers are idempotent",
                USER_COUNT, CONSULTANT_COUNT, MANAGER_COUNT);
    }

    private User account(String kind, int index, Role role, Department department, Faculty faculty) {
        String baseEmail = String.format(Locale.ROOT, "demo.%s%02d%s", kind, index, EMAIL_DOMAIN);
        User user = findOrCreateAccount(baseEmail, role, index);
        user.setFullName(DemoPersonNames.forRole(role, index));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        user.setDeleted(false);
        if (faculty != null) user.setFaculty(faculty);
        if (role == Role.USER) {
            user.setStudent(true);
            user.setCurrentlyStudying(true);
            user.setStudentCode(String.format(Locale.ROOT, "DEMO%04d", index));
            user.setStudentId(user.getStudentCode());
        } else {
            user.setStudent(false);
            user.setCurrentlyStudying(false);
            user.setStudentCode(null);
            user.setStudentId(null);
        }
        return users.save(user);
    }

    private User findOrCreateAccount(String baseEmail, Role role, int index) {
        String localPart = baseEmail.substring(0, baseEmail.indexOf('@'));
        for (int suffix = 0; suffix < 100; suffix++) {
            String email = suffix == 0 ? baseEmail : localPart + ".seed" + suffix + EMAIL_DOMAIN;
            Optional<User> existing = users.findByEmailIgnoreCase(email);
            if (existing.isPresent()) {
                User user = existing.get();
                if (user.getRole() == role && user.getStatus() == UserStatus.ACTIVE
                        && user.isEnabled() && !user.isDeleted()) return user;
                continue;
            }

            User user = new User();
            user.setEmail(email);
            user.setFullName(DemoPersonNames.forRole(role, index));
            user.setPasswordHash(passwordEncoder.encode(demoPassword));
            user.setRole(role);
            user.setStatus(UserStatus.ACTIVE);
            user.setEnabled(true);
            user.setDeleted(false);
            return user;
        }
        throw new IllegalStateException("Could not allocate a local demo email for " + baseEmail);
    }

    private Faculty findFaculty(Department department, List<Faculty> facultyList) {
        String departmentName = normalize(department.getName());
        return facultyList.stream()
                .filter(faculty -> {
                    String facultyName = normalize(faculty.getName());
                    return facultyName.equals(departmentName) || facultyName.startsWith(departmentName)
                            || departmentName.startsWith(facultyName);
                })
                .findFirst().orElse(null);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", "");
    }

    private void createQuestionsAndAnswers(List<Department> departmentList,
            Map<Long, Faculty> facultyByDepartment, Map<Long, List<FacultyTopic>> topicsByFaculty,
            List<User> demoUsers, List<User> consultants, List<User> managers) {
        Map<String, Question> existingQuestions = new HashMap<>();
        for (Question question : questions.findAll()) {
            if (question.getSubject() != null && question.getSubject().startsWith("[QAUTE-DEMO-Q")) {
                existingQuestions.put(question.getSubject().substring(0, 16), question);
            }
        }

        Map<String, Answer> existingAnswers = new HashMap<>();
        for (Answer answer : answers.findAll()) {
            if (answer.getBody() != null && answer.getBody().startsWith("[QAUTE-DEMO-A")) {
                int end = answer.getBody().indexOf(']');
                if (end > 0) existingAnswers.put(answer.getBody().substring(0, end + 1), answer);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < QUESTION_SUBJECTS.size(); i++) {
            int number = i + 1;
            String marker = String.format(Locale.ROOT, "[QAUTE-DEMO-Q%02d]", number);
            Question question = existingQuestions.get(marker);
            if (question == null) {
                Department department = departmentList.get(i % departmentList.size());
                Faculty faculty = facultyByDepartment.get(department.getId());
                List<FacultyTopic> matchingTopics = faculty == null ? List.of()
                        : topicsByFaculty.getOrDefault(faculty.getId(), List.of());
                FacultyTopic topic = matchingTopics.isEmpty() ? null : matchingTopics.get(i % matchingTopics.size());
                question = new Question(demoUsers.get(i % demoUsers.size()), faculty,
                        marker + " " + QUESTION_SUBJECTS.get(i), "", now.minusDays(i % 20).minusHours(i % 7));
                question.setDepartment(department);
                question.setTopic(topic);
                question.setBody("Em đang cần tư vấn về: " + QUESTION_SUBJECTS.get(i)
                        + ". Nhờ thầy cô hướng dẫn các bước thực hiện và thời hạn liên quan.");
                question.setCategory(topic == null ? "Tư vấn sinh viên" : topic.getName());
                question.setAudience(AudienceType.SINH_VIEN);
                question.setStatus(QuestionStatus.UNANSWERED);
                question.setPublic(i % 3 != 1);
                question.setLocked(false);
                question.setReplyNotification(false);
                question.setDeleted(false);
                question = questions.save(question);
                existingQuestions.put(marker, question);
            }

            int answerSlots = number <= 12 && number % 3 == 1 ? 2 : 1;
            if (number >= 19) answerSlots = 1;
            boolean hasPublishedAnswer = false;
            for (int slot = 1; slot <= answerSlots; slot++) {
                String answerMarker = String.format(Locale.ROOT, "[QAUTE-DEMO-A%02d-%d]", number, slot);
                Answer existing = existingAnswers.get(answerMarker);
                if (existing != null) {
                    if (existing.isPublished()) hasPublishedAnswer = true;
                    continue;
                }

                boolean pending = number >= 19;
                Department questionDepartment = departmentList.get((number - 1) % departmentList.size());
                User managerForQuestion = findManagerForDepartment(managers, questionDepartment);
                boolean managerReply = !pending && managerForQuestion != null && ((number + slot) % 4 == 0);
                User author = managerReply
                        ? managerForQuestion
                        : findConsultantForDepartment(consultants, questionDepartment,
                                (number + slot) % consultants.size());
                ApprovalStatus status = pending ? ApprovalStatus.PENDING
                        : managerReply ? ApprovalStatus.NOT_REQUIRED : ApprovalStatus.APPROVED;
                String response = managerReply
                        ? "Phòng phụ trách đã ghi nhận yêu cầu. Em vui lòng xem thông báo trên cổng sinh viên và bổ sung minh chứng nếu cần."
                        : "Em kiểm tra mục hướng dẫn trên cổng sinh viên trước. Nếu vẫn chưa được, hãy gửi mã sinh viên và ảnh chụp màn hình để khoa kiểm tra.";
                Answer answer = new Answer(question, author,
                        answerMarker + " " + response,
                        question.getCreatedAt().plusHours(slot + 1), status);
                answer.setReviewReason(null);
                answers.save(answer);
                existingAnswers.put(answerMarker, answer);
                if (answer.isPublished()) hasPublishedAnswer = true;
            }
            question.setStatus(hasPublishedAnswer ? QuestionStatus.ANSWERED : QuestionStatus.UNANSWERED);
        }
    }

    private User findConsultantForDepartment(List<User> consultants, Department department, int fallbackIndex) {
        return consultants.stream()
                .filter(consultant -> consultant.getFaculty() != null
                        && findFaculty(department, List.of(consultant.getFaculty())) != null)
                .findFirst().orElse(consultants.get(fallbackIndex));
    }

    private User findManagerForDepartment(List<User> managers, Department department) {
        return managers.stream()
                .filter(manager -> manager.getManagedDepartments().stream()
                        .anyMatch(assigned -> department.getId().equals(assigned.getId())))
                .findFirst().orElse(null);
    }
}
