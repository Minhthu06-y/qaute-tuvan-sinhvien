package vn.iotstar.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.iotstar.entity.User;
import vn.iotstar.model.*;
import vn.iotstar.enums.*;
import vn.iotstar.enums.AdvisorStatus;
import vn.iotstar.repository.AdvisorProfileRepository;
import vn.iotstar.repository.AnswerRepository;
import vn.iotstar.repository.QuestionRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.NotificationService;
import vn.iotstar.service.ScopeService;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final UserRepository users;
    private final AdvisorProfileRepository advisors;
    private final NotificationService notifications;
    private final ScopeService scope;
    public ApiController(QuestionRepository questions, AnswerRepository answers, UserRepository users,
            AdvisorProfileRepository advisors, NotificationService notifications, ScopeService scope) {
        this.questions = questions; this.answers = answers; this.users = users; this.advisors = advisors;
        this.notifications = notifications; this.scope = scope;
    }

    @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "ok", "application", "QAUTE - Nhóm 4"); }

    @GetMapping("/questions") public List<QuestionDto> list(Authentication authentication) {
        User user = currentUser(authentication);
        List<Question> result;
        if (isManager(authentication)) {
            var allowed = scope.managerDepartments(user.getEmail());
            result = questions.findAllByOrderByCreatedAtDesc().stream()
                    .filter(question -> !question.isDeleted())
                    .filter(question -> ScopeService.allows(allowed, question.getDepartment())).toList();
        } else if (user.getRole() == Role.ADMIN) {
            result = questions.findAllByOrderByCreatedAtDesc().stream().filter(question -> !question.isDeleted()).toList();
        } else if (isAdvisor(authentication)) {
            result = user.getFaculty() == null ? List.of()
                    : questions.findByFacultyIdOrderByCreatedAtDesc(user.getFaculty().getId()).stream()
                            .filter(question -> !question.isDeleted())
                            .filter(question -> user.isAssignedTo(question.getTopic())).toList();
        } else {
            result = questions.findByAskerIdOrderByCreatedAtDesc(user.getId()).stream()
                    .filter(question -> !question.isDeleted()).toList();
        }
        return result.stream().map(this::toDto).toList();
    }

    @GetMapping("/questions/{id}") public QuestionDto get(@PathVariable Long id, Authentication authentication) {
        Question question = questions.findById(id).filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureCanView(question, authentication);
        return toDto(question);
    }

    @PostMapping("/questions/{id}/answers") public AnswerDto reply(@PathVariable Long id, @RequestBody AnswerRequest request, Authentication authentication) {
        User author = currentUser(authentication);
        Question question = questions.findById(id).filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureCanManage(question, authentication, author);
        if (question.isLocked() && !isManager(authentication)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Question is locked after transfer");
        if (request.body() == null || request.body().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer body is required");
        boolean approvalRequired = !isManager(authentication) && isAdvisor(authentication) && request.requestApproval();
        ApprovalStatus state = approvalRequired ? ApprovalStatus.PENDING : ApprovalStatus.NOT_REQUIRED;
        Answer answer = answers.save(new Answer(question, author, request.body().trim(), LocalDateTime.now(), state));
        if (!approvalRequired) {
            question.setStatus(QuestionStatus.ANSWERED); question.setReplyNotification(true);
            if (isManager(authentication)) question.setLocked(false);
            questions.save(question);
            if (question.getAsker() != null)
                notifications.notifyUser(question.getAsker().getId(), "reply",
                        "Question received a new answer.", question.getId());
        } else {
            if (question.getFaculty() != null)
                notifications.notifyManagers(question.getFaculty().getId(), "approval",
                        "An answer is waiting for review.", question.getId());
        }
        return new AnswerDto(answer.getId(), author.getDisplayName(), answer.getBody(), answer.getCreatedAt(), answer.getApprovalStatus().name());
    }

    private User currentUser(Authentication authentication) {
        return users.findByEmail(authentication.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    private boolean hasAuthority(Authentication auth, String authority) { return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority)); }
    private boolean isAdvisor(Authentication auth) {
        if (!hasAuthority(auth, "ROLE_CONSULTANT")) return false;
        return users.findByEmailIgnoreCase(auth.getName())
                .map(user -> advisors.existsByUserIdAndStatusAndDeletedFalse(user.getId(), AdvisorStatus.ACTIVE))
                .orElse(false);
    }
    private boolean isManager(Authentication auth) { return hasAuthority(auth, "ROLE_MANAGER"); }
    private void ensureCanView(Question q, Authentication auth) {
        if (q.isDeleted()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        User user = currentUser(auth);
        if (user.getRole() == Role.ADMIN) {
            return;
        } else if (isManager(auth)) {
            if (!ScopeService.allows(scope.managerDepartments(user.getEmail()), q.getDepartment()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            return;
        } else if (isAdvisor(auth)) {
            boolean assignedQuestion = user.getFaculty() != null && q.getFaculty() != null
                    && user.getFaculty().getId().equals(q.getFaculty().getId())
                    && user.isAssignedTo(q.getTopic());
            if (!assignedQuestion) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } else if (!q.getAsker().getId().equals(user.getId()) && !q.isPublic()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
    private void ensureCanManage(Question q, Authentication auth, User user) {
        if (isManager(auth)) {
            if (!ScopeService.allows(scope.managerDepartments(user.getEmail()), q.getDepartment()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            return;
        }
        boolean sameFaculty = user.getFaculty() != null && q.getFaculty() != null
                && user.getFaculty().getId().equals(q.getFaculty().getId());
        boolean staff = sameFaculty && isAdvisor(auth) && user.isAssignedTo(q.getTopic());
        if (!staff) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private QuestionDto toDto(Question q) {
        List<AnswerDto> answerDtos = answers.findByQuestionIdOrderByCreatedAtAsc(q.getId()).stream()
                .filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED || a.getApprovalStatus() == ApprovalStatus.NOT_REQUIRED)
                .map(a -> new AnswerDto(a.getId(), a.getAuthor().getDisplayName(), a.getBody(), a.getCreatedAt(), a.getApprovalStatus().name())).toList();
        return new QuestionDto(q.getId(), q.getSubject(), q.getBody(), q.getAsker().getDisplayName(),
                q.getFaculty() == null ? null : q.getFaculty().getName(),
                q.getTopic() == null ? null : q.getTopic().getName(), q.getCreatedAt(), q.getStatus().name(), q.isPublic(), q.isLocked(), q.isReplyNotification(), answerDtos);
    }
    public record AnswerRequest(String body, boolean requestApproval) {}
    public record AnswerDto(Long id, String author, String body, LocalDateTime createdAt, String approvalStatus) {}
    public record QuestionDto(Long id, String subject, String body, String asker, String faculty, String topic, LocalDateTime createdAt,
            String status, boolean isPublic, boolean locked, boolean replyNotification, List<AnswerDto> answers) {}
}
