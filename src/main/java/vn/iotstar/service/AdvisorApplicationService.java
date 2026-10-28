package vn.iotstar.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.iotstar.dto.AdvisorApplicationForm;
import vn.iotstar.entity.AdvisorApplication;
import vn.iotstar.entity.AdvisorProfile;
import vn.iotstar.entity.Department;
import vn.iotstar.entity.User;
import vn.iotstar.enums.AdvisorApplicationStatus;
import vn.iotstar.enums.AdvisorStatus;
import vn.iotstar.enums.AdvisorType;
import vn.iotstar.enums.Role;
import vn.iotstar.enums.UserStatus;
import vn.iotstar.model.Faculty;
import vn.iotstar.model.FacultyTopic;
import vn.iotstar.repository.AdvisorApplicationRepository;
import vn.iotstar.repository.AdvisorProfileRepository;
import vn.iotstar.repository.DepartmentRepository;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.FacultyTopicRepository;
import vn.iotstar.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AdvisorApplicationService {
    private final AdvisorApplicationRepository applications;
    private final AdvisorProfileRepository profiles;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final FacultyRepository faculties;
    private final FacultyTopicRepository topics;
    private final ScopeService scope;
    private final NotificationService notifications;
    private final ActivityLogService logs;

    @Transactional(readOnly = true)
    public AdvisorApplication latest(Long applicantId) {
        return applications.findFirstByApplicantIdOrderByCreatedAtDesc(applicantId).orElse(null);
    }

    @Transactional
    public AdvisorApplication submit(User applicant, AdvisorApplicationForm form) {
        if (applicant.getRole() != Role.USER)
            throw new FieldException("advisorType", "Chỉ tài khoản người dùng mới có thể gửi yêu cầu tư vấn viên");
        if (profiles.findByUserId(applicant.getId()).filter(profile -> !profile.isDeleted()).isPresent())
            throw new FieldException("advisorType", "Tài khoản đã có hồ sơ tư vấn viên");
        if (applications.findFirstByApplicantIdAndStatusOrderByCreatedAtDesc(
                applicant.getId(), AdvisorApplicationStatus.PENDING).isPresent())
            throw new FieldException("departmentId", "Bạn đã có yêu cầu đang chờ duyệt");
        if (form.getAdvisorType() == AdvisorType.STUDENT || form.getAdvisorType() == AdvisorType.TEAM_LEADER)
            throw new FieldException("advisorType", "Chọn chức danh tư vấn viên phù hợp");

        Department department = departments.findById(form.getDepartmentId())
                .orElseThrow(() -> new FieldException("departmentId", "Khoa / đơn vị không tồn tại"));
        Faculty faculty = facultyFor(department);
        FacultyTopic topic = topics.findById(form.getTopicId())
                .orElseThrow(() -> new FieldException("topicId", "Chuyên đề không tồn tại"));
        if (!topic.getFaculty().getId().equals(faculty.getId()))
            throw new FieldException("topicId", "Chuyên đề không thuộc khoa / đơn vị đã chọn");

        AdvisorApplication application = new AdvisorApplication();
        application.setApplicant(applicant);
        application.setDepartment(department);
        application.setTopic(topic);
        application.setAdvisorType(form.getAdvisorType());
        application.setAudience(form.getAudience());
        application.setMotivation(form.getMotivation() == null ? null : form.getMotivation().trim());
        if (form.getPhone() != null && !form.getPhone().isBlank()) applicant.setPhone(form.getPhone().trim());
        users.save(applicant);
        AdvisorApplication saved = applications.save(application);
        notifications.notifyManagers(faculty.getId(), "advisor-application",
                "Có yêu cầu mới làm tư vấn viên cần xem xét.", null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AdvisorApplication> pendingForManager(String managerEmail) {
        Set<Long> allowed = scope.managerDepartments(managerEmail);
        return applications.findByStatusOrderByCreatedAtAsc(AdvisorApplicationStatus.PENDING).stream()
                .filter(application -> ScopeService.allows(allowed, application.getDepartment()))
                .toList();
    }

    @Transactional
    public void approve(Long id, String managerEmail) {
        AdvisorApplication application = getPendingForManager(id, managerEmail);
        User applicant = application.getApplicant();
        if (applicant.getRole() != Role.USER)
            throw new FieldException("decision", "Tài khoản không còn đủ điều kiện làm tư vấn viên");
        Faculty faculty = facultyFor(application.getDepartment());
        AdvisorProfile profile = profiles.findByUserId(applicant.getId()).orElseGet(AdvisorProfile::new);
        profile.setUser(applicant);
        profile.setDepartment(application.getDepartment());
        profile.setAdvisorType(application.getAdvisorType());
        profile.setAudience(application.getAudience());
        profile.setStatus(AdvisorStatus.ACTIVE);
        profile.setHead(false);
        profile.setDeleted(false);
        profile.setDeletedAt(null);
        profile.setDeletedBy(null);

        applicant.setRole(Role.CONSULTANT);
        applicant.setStatus(UserStatus.ACTIVE);
        applicant.setEnabled(true);
        applicant.setFaculty(faculty);
        applicant.setTitle(application.getAdvisorType().getLabel());
        applicant.setConsultantTitles(Set.of(application.getAdvisorType()));
        applicant.setAssignedTopics(Set.of(application.getTopic()));
        users.save(applicant);
        profiles.save(profile);
        finish(application, managerEmail, AdvisorApplicationStatus.APPROVED, "Đã duyệt yêu cầu làm tư vấn viên.");
        notifications.notify(applicant, "Yêu cầu làm tư vấn viên đã được duyệt.", "/questions");
        logs.log(managerEmail, "MANAGER", "APPROVE_ADVISOR_APPLICATION", applicant.getEmail());
    }

    @Transactional
    public void reject(Long id, String managerEmail, String note) {
        AdvisorApplication application = getPendingForManager(id, managerEmail);
        if (note == null || note.isBlank())
            throw new FieldException("reviewNote", "Nhập lý do từ chối để người gửi biết cần bổ sung gì");
        finish(application, managerEmail, AdvisorApplicationStatus.REJECTED, note.trim());
        notifications.notify(application.getApplicant(), "Yêu cầu làm tư vấn viên chưa được duyệt: " + note.trim(),
                "/advisor-application");
        logs.log(managerEmail, "MANAGER", "REJECT_ADVISOR_APPLICATION", application.getApplicant().getEmail());
    }

    private AdvisorApplication getPendingForManager(Long id, String managerEmail) {
        AdvisorApplication application = applications.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu tư vấn viên"));
        if (application.getStatus() != AdvisorApplicationStatus.PENDING)
            throw new IllegalArgumentException("Yêu cầu này đã được xử lý");
        if (!ScopeService.allows(scope.managerDepartments(managerEmail), application.getDepartment()))
            throw new AccessDeniedException("Yêu cầu không thuộc khoa bạn phụ trách");
        return application;
    }

    private void finish(AdvisorApplication application, String managerEmail,
            AdvisorApplicationStatus status, String note) {
        User reviewer = users.findByEmailIgnoreCase(managerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản Manager"));
        application.setStatus(status);
        application.setReviewer(reviewer);
        application.setReviewNote(note);
        application.setReviewedAt(LocalDateTime.now());
        applications.save(application);
    }

    private Faculty facultyFor(Department department) {
        return faculties.findAll().stream()
                .filter(faculty -> faculty.getName().trim().equalsIgnoreCase(department.getName().trim()))
                .findFirst().orElseThrow(() -> new FieldException("departmentId",
                        "Khoa / đơn vị chưa được liên kết với danh mục khoa tư vấn"));
    }
}
