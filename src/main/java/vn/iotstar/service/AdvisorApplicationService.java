package vn.iotstar.service;

import java.time.LocalDateTime;
import java.util.HashSet;
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
import vn.iotstar.enums.AudienceType;
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
                    applicant.getId(), AdvisorApplicationStatus.PENDING).isPresent()
                || applications.findFirstByApplicantIdAndStatusOrderByCreatedAtDesc(
                    applicant.getId(), AdvisorApplicationStatus.MANAGER_APPROVED).isPresent())
            throw new FieldException("departmentId", "Bạn đã có yêu cầu đang chờ duyệt");
        if (form.getAdvisorType() == AdvisorType.TEAM_LEADER)
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
        AdvisorApplication saved = applications.save(application);
        notifications.notifyManagers(faculty.getId(), "advisor-application",
                "Có yêu cầu đăng ký tư vấn viên mới cần duyệt.", null, "/manager/advisor-applications");
        logs.log(applicant.getEmail(), Role.USER.name(), "SUBMIT_ADVISOR_APPLICATION", applicant.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AdvisorApplication> pendingForManager(String managerEmail) {
        Set<Long> allowed = scope.managerDepartments(managerEmail);
        return applications.findByStatusOrderByCreatedAtAsc(AdvisorApplicationStatus.PENDING).stream()
                .filter(application -> ScopeService.allows(allowed, application.getDepartment()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdvisorApplication> approvedByManagersForAdmin() {
        return applications.findByStatusOrderByCreatedAtAsc(AdvisorApplicationStatus.MANAGER_APPROVED);
    }

    @Transactional
    public void approveByManager(Long id, String managerEmail) {
        User reviewer = getReviewer(managerEmail, Role.MANAGER);
        AdvisorApplication application = getForReviewer(id, reviewer, AdvisorApplicationStatus.PENDING);
        User applicant = application.getApplicant();
        if (applicant.getRole() != Role.USER)
            throw new FieldException("decision", "Tài khoản không còn đủ điều kiện làm tư vấn viên");
        finish(application, reviewer, AdvisorApplicationStatus.MANAGER_APPROVED,
                "Manager đã duyệt hồ sơ và chuyển admin cấp quyền.");
        notifications.notifyAdmins("advisor-application",
                "Manager đã duyệt hồ sơ tư vấn viên; đang chờ admin cấp quyền.",
                "/admin/advisor-applications");
        notifications.notify(applicant,
                "Manager đã duyệt hồ sơ. Yêu cầu đang chờ admin cấp quyền tư vấn viên.",
                "/advisor-application");
        logs.log(reviewer.getEmail(), reviewer.getRole().name(), "MANAGER_APPROVE_ADVISOR_APPLICATION", applicant.getEmail());
    }

    @Transactional
    public void rejectByManager(Long id, String managerEmail, String note) {
        User reviewer = getReviewer(managerEmail, Role.MANAGER);
        AdvisorApplication application = getForReviewer(id, reviewer, AdvisorApplicationStatus.PENDING);
        if (note == null || note.isBlank())
            throw new FieldException("reviewNote", "Nhập lý do từ chối để người gửi biết cần bổ sung gì");
        finish(application, reviewer, AdvisorApplicationStatus.REJECTED, note.trim());
        notifications.notify(application.getApplicant(), "Yêu cầu làm tư vấn viên chưa được duyệt: " + note.trim(),
                "/advisor-application");
        logs.log(reviewer.getEmail(), reviewer.getRole().name(), "MANAGER_REJECT_ADVISOR_APPLICATION", application.getApplicant().getEmail());
    }

    @Transactional
    public void grantByAdmin(Long id, String adminEmail) {
        User reviewer = getReviewer(adminEmail, Role.ADMIN);
        AdvisorApplication application = getForReviewer(id, reviewer, AdvisorApplicationStatus.MANAGER_APPROVED);
        User applicant = application.getApplicant();
        if (applicant.getRole() != Role.USER)
            throw new FieldException("decision", "Tài khoản không còn đủ điều kiện làm tư vấn viên");
        Faculty faculty = facultyFor(application.getDepartment());
        activateApplicant(applicant, faculty, application.getDepartment(), application.getTopic(),
                application.getAdvisorType(), application.getAudience());
        finish(application, reviewer, AdvisorApplicationStatus.APPROVED,
                "Admin đã cấp quyền tư vấn viên sau khi manager duyệt hồ sơ.");
        notifications.notify(applicant, "Admin đã cấp quyền tư vấn viên cho tài khoản của bạn.", "/questions");
        logs.log(reviewer.getEmail(), reviewer.getRole().name(), "GRANT_ADVISOR_ROLE", applicant.getEmail());
    }

    @Transactional
    public void rejectByAdmin(Long id, String adminEmail, String note) {
        User reviewer = getReviewer(adminEmail, Role.ADMIN);
        AdvisorApplication application = getForReviewer(id, reviewer, AdvisorApplicationStatus.MANAGER_APPROVED);
        if (note == null || note.isBlank())
            throw new FieldException("reviewNote", "Nhập lý do từ chối để người gửi biết cần bổ sung gì");
        finish(application, reviewer, AdvisorApplicationStatus.REJECTED, note.trim());
        notifications.notify(application.getApplicant(),
                "Admin chưa cấp quyền tư vấn viên: " + note.trim(), "/advisor-application");
        logs.log(reviewer.getEmail(), reviewer.getRole().name(), "ADMIN_REJECT_ADVISOR_APPLICATION",
                application.getApplicant().getEmail());
    }

    private User getReviewer(String reviewerEmail, Role expectedRole) {
        User reviewer = users.findByEmailIgnoreCase(reviewerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản duyệt yêu cầu"));
        if (reviewer.getRole() != expectedRole)
            throw new AccessDeniedException("Không đủ quyền xử lý bước duyệt này");
        return reviewer;
    }

    private AdvisorApplication getForReviewer(Long id, User reviewer, AdvisorApplicationStatus expectedStatus) {
        AdvisorApplication application = applications.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu tư vấn viên"));
        if (application.getStatus() != expectedStatus)
            throw new IllegalArgumentException("Yêu cầu này đã được xử lý");
        if (reviewer.getRole() == Role.MANAGER
                && !ScopeService.allows(scope.managerDepartments(reviewer.getEmail()), application.getDepartment()))
            throw new AccessDeniedException("Yêu cầu không thuộc khoa bạn phụ trách");
        return application;
    }

    private void finish(AdvisorApplication application, User reviewer,
            AdvisorApplicationStatus status, String note) {
        application.setStatus(status);
        application.setReviewer(reviewer);
        application.setReviewNote(note);
        application.setReviewedAt(LocalDateTime.now());
        applications.save(application);
    }

    private void activateApplicant(User applicant, Faculty faculty, Department department,
            FacultyTopic topic, AdvisorType advisorType, AudienceType audience) {
        AdvisorProfile profile = profiles.findByUserId(applicant.getId()).orElseGet(AdvisorProfile::new);
        profile.setUser(applicant);
        profile.setDepartment(department);
        profile.setAdvisorType(advisorType);
        profile.setAudience(audience);
        profile.setStatus(AdvisorStatus.ACTIVE);
        profile.setHead(false);
        profile.setDeleted(false);
        profile.setDeletedAt(null);
        profile.setDeletedBy(null);

        applicant.setRole(Role.CONSULTANT);
        applicant.setStatus(UserStatus.ACTIVE);
        applicant.setEnabled(true);
        applicant.setFaculty(faculty);
        applicant.setTitle(advisorType.getLabel());
        applicant.setConsultantTitles(new HashSet<>(Set.of(advisorType)));
        applicant.setAssignedTopics(Set.of(topic));
        users.save(applicant);
        profiles.save(profile);
    }

    private Faculty facultyFor(Department department) {
        return faculties.findAll().stream()
                .filter(faculty -> faculty.getName().trim().equalsIgnoreCase(department.getName().trim()))
                .findFirst().orElseThrow(() -> new FieldException("departmentId",
                        "Khoa / đơn vị chưa được liên kết với danh mục khoa tư vấn"));
    }
}
