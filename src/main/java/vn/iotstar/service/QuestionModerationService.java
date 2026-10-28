package vn.iotstar.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.iotstar.entity.QuestionReport;
import vn.iotstar.entity.User;
import vn.iotstar.enums.QuestionReportStatus;
import vn.iotstar.enums.Role;
import vn.iotstar.model.Question;
import vn.iotstar.repository.QuestionReportRepository;
import vn.iotstar.repository.QuestionRepository;
import vn.iotstar.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class QuestionModerationService {
    private final QuestionReportRepository reports;
    private final QuestionRepository questions;
    private final UserRepository users;
    private final ScopeService scope;
    private final NotificationService notifications;
    private final ActivityLogService logs;

    @Transactional
    public void report(Long questionId, User reporter, String reason) {
        if (reporter.getRole() != Role.CONSULTANT)
            throw new AccessDeniedException("Chỉ tư vấn viên mới có thể báo cáo câu hỏi");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Vui lòng nhập lý do báo cáo");
        if (reason.trim().length() > 2000) throw new IllegalArgumentException("Lý do báo cáo tối đa 2.000 ký tự");
        Question question = questions.findById(questionId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi"));
        if (reports.existsByQuestionIdAndReporterIdAndStatus(questionId, reporter.getId(), QuestionReportStatus.PENDING))
            throw new IllegalArgumentException("Bạn đã báo cáo câu hỏi này; Manager đang xem xét");
        QuestionReport report = new QuestionReport();
        report.setQuestion(question);
        report.setReporter(reporter);
        report.setReason(reason.trim());
        reports.save(report);
        if (question.getFaculty() != null)
            notifications.notifyManagers(question.getFaculty().getId(), "question-report",
                    "Tư vấn viên vừa báo cáo một câu hỏi.", question.getId());
        logs.log(reporter.getEmail(), Role.CONSULTANT.name(), "REPORT_QUESTION", "question#" + questionId);
    }

    @Transactional(readOnly = true)
    public List<QuestionReport> pendingForManager(String managerEmail) {
        Set<Long> allowed = scope.managerDepartments(managerEmail);
        return reports.findByStatusOrderByCreatedAtDesc(QuestionReportStatus.PENDING).stream()
                .filter(report -> !report.getQuestion().isDeleted())
                .filter(report -> ScopeService.allows(allowed, report.getQuestion().getDepartment()))
                .toList();
    }

    @Transactional
    public void decide(Long reportId, String managerEmail, boolean deleteQuestion, String note) {
        QuestionReport report = getPendingForManager(reportId, managerEmail);
        User manager = users.findByEmailIgnoreCase(managerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản Manager"));
        Question question = report.getQuestion();
        LocalDateTime now = LocalDateTime.now();
        if (deleteQuestion) {
            question.setDeleted(true);
            question.setDeletedAt(now);
            question.setDeletedBy(managerEmail);
            QuestionReportStatus status = QuestionReportStatus.DELETED;
            report.setStatus(status);
            reports.findByQuestionIdAndStatus(question.getId(), QuestionReportStatus.PENDING).forEach(other -> {
                other.setStatus(status);
                other.setReviewer(manager);
                other.setReviewNote(note == null || note.isBlank() ? "Câu hỏi đã bị gỡ khỏi hệ thống." : note.trim());
                other.setReviewedAt(now);
            });
            notifications.notify(question.getAsker(), "Câu hỏi của bạn đã được gỡ sau khi Manager xem xét báo cáo.", null);
        } else {
            report.setStatus(QuestionReportStatus.DISMISSED);
            notifications.notify(report.getReporter(), "Manager đã xem xét và giữ lại câu hỏi bạn báo cáo.",
                    "/questions/" + question.getId());
        }
        report.setReviewer(manager);
        report.setReviewNote(note == null || note.isBlank() ? null : note.trim());
        report.setReviewedAt(now);
        reports.save(report);
        logs.log(managerEmail, Role.MANAGER.name(), deleteQuestion ? "DELETE_REPORTED_QUESTION" : "DISMISS_QUESTION_REPORT",
                "question#" + question.getId());
    }

    @Transactional
    public void deleteByAdmin(Long questionId, String adminEmail) {
        Question question = questions.findById(questionId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi"));
        question.setDeleted(true);
        question.setDeletedAt(LocalDateTime.now());
        question.setDeletedBy(adminEmail);
        User admin = users.findByEmailIgnoreCase(adminEmail).orElse(null);
        reports.findByQuestionIdAndStatus(questionId, QuestionReportStatus.PENDING).forEach(report -> {
            report.setStatus(QuestionReportStatus.DELETED);
            report.setReviewer(admin);
            report.setReviewNote("Câu hỏi đã được Admin gỡ khỏi hệ thống.");
            report.setReviewedAt(LocalDateTime.now());
        });
        logs.log(adminEmail, Role.ADMIN.name(), "DELETE_QUESTION", "question#" + questionId);
    }

    private QuestionReport getPendingForManager(Long reportId, String managerEmail) {
        QuestionReport report = reports.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy báo cáo"));
        if (report.getStatus() != QuestionReportStatus.PENDING || report.getQuestion().isDeleted())
            throw new IllegalArgumentException("Báo cáo này đã được xử lý");
        if (!ScopeService.allows(scope.managerDepartments(managerEmail), report.getQuestion().getDepartment()))
            throw new AccessDeniedException("Báo cáo không thuộc khoa bạn phụ trách");
        return report;
    }
}
