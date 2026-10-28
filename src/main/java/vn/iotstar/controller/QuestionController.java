package vn.iotstar.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import vn.iotstar.entity.AdvisorProfile;
import vn.iotstar.enums.AdvisorStatus;
import vn.iotstar.enums.AdvisorType;
import vn.iotstar.enums.Role;
import vn.iotstar.model.*;
import vn.iotstar.enums.*;
import vn.iotstar.repository.*;
import vn.iotstar.service.AdvisorPresenceService;
import vn.iotstar.service.CloudinaryStorageService;
import vn.iotstar.service.NotificationService;
import vn.iotstar.service.SessionContext;
import vn.iotstar.service.ScopeService;

@Controller
public class QuestionController {
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final FacultyRepository faculties;
    private final DepartmentRepository departments;
    private final FacultyTopicRepository topics;
    private final QuestionTransferRepository transfers;
    private final SessionContext sessionContext;
    private final NotificationService notifications;
    private final CloudinaryStorageService cloudinary;
    private final UserRepository users;
    private final AdvisorProfileRepository advisorProfiles;
    private final AdvisorPresenceService advisorPresence;
    private final ScopeService scope;

    public QuestionController(QuestionRepository questions, AnswerRepository answers, FacultyRepository faculties,
            DepartmentRepository departments, FacultyTopicRepository topics,
            QuestionTransferRepository transfers, SessionContext sessionContext, NotificationService notifications,
            CloudinaryStorageService cloudinary, UserRepository users, AdvisorProfileRepository advisorProfiles,
            AdvisorPresenceService advisorPresence, ScopeService scope) {
        this.questions = questions; this.answers = answers; this.faculties = faculties; this.departments = departments;
        this.topics = topics; this.transfers = transfers;
        this.sessionContext = sessionContext; this.notifications = notifications; this.cloudinary = cloudinary;
        this.users = users; this.advisorProfiles = advisorProfiles; this.advisorPresence = advisorPresence;
        this.scope = scope;
    }

    @GetMapping("/questions") public String dashboard(@RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long topicId, HttpSession session, Model model) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (viewer == null) return "redirect:/login";
        List<Question> all;
        List<FacultyTopic> availableTopics;
        List<Faculty> visibleFaculties = List.of();
        Set<Long> allowedManagerDepartments = viewer.isManager()
                ? scope.managerDepartments(viewer.user().getEmail()) : Set.of();
        Long selectedFacultyId = null;
        Long selectedTopicId = null;
        if (viewer.isStudent()) {
            all = questions.findByAskerIdOrderByCreatedAtDesc(viewer.user().getId());
            availableTopics = List.of();
        } else if (viewer.role() == Role.ADMIN) {
            all = questions.findAllByOrderByCreatedAtDesc();
            availableTopics = topics.findAllByOrderByFacultyIdAscIdAsc();
        } else if (viewer.isManager()) {
            List<vn.iotstar.entity.Department> assignedDepartments = departments.findAllById(allowedManagerDepartments);
            visibleFaculties = faculties.findAll().stream()
                    .filter(faculty -> assignedDepartments.stream().anyMatch(department ->
                            department.getName().trim().equalsIgnoreCase(faculty.getName().trim())))
                    .toList();
            selectedFacultyId = facultyId != null && visibleFaculties.stream()
                    .anyMatch(faculty -> faculty.getId().equals(facultyId)) ? facultyId : null;
            availableTopics = visibleFaculties.stream()
                    .flatMap(faculty -> topics.findByFacultyIdOrderByIdAsc(faculty.getId()).stream())
                    .toList();
            all = questions.findAllByOrderByCreatedAtDesc().stream()
                    .filter(question -> ScopeService.allows(allowedManagerDepartments, question.getDepartment()))
                    .toList();
            if (selectedFacultyId != null) {
                Long selectedId = selectedFacultyId;
                all = all.stream().filter(question -> question.getFaculty() != null
                        && question.getFaculty().getId().equals(selectedId)).toList();
            }
            if (topicId != null && availableTopics.stream().anyMatch(topic -> topic.getId().equals(topicId))) {
                selectedTopicId = topicId;
                all = all.stream().filter(question -> question.getTopic() != null
                        && question.getTopic().getId().equals(topicId)).toList();
            }
        } else {
            Long consultantFacultyId = viewer.user().getFaculty() == null ? null : viewer.user().getFaculty().getId();
            all = consultantFacultyId == null ? List.of()
                    : questions.findByFacultyIdAndLockedFalseOrderByCreatedAtDesc(consultantFacultyId).stream()
                            .filter(question -> canHandle(viewer, question)).toList();
            availableTopics = consultantFacultyId == null ? List.of() : viewer.user().getAssignedTopics().stream()
                    .filter(topic -> topic.getFaculty() != null && topic.getFaculty().getId().equals(consultantFacultyId))
                    .sorted(java.util.Comparator.comparing(FacultyTopic::getId)).toList();
        }
        List<Question> unanswered = all.stream().filter(q -> q.getStatus() == QuestionStatus.OPEN
                || q.getStatus() == QuestionStatus.UNANSWERED).toList();
        List<Question> answered = all.stream().filter(q -> q.getStatus() == QuestionStatus.ANSWERED).toList();
        model.addAttribute("unanswered", unanswered);
        model.addAttribute("answered", answered);
        model.addAttribute("studentQuestions", all);
        model.addAttribute("topics", availableTopics);
        model.addAttribute("selectedTopicId", selectedTopicId);
        List<Question> transferred = viewer.isManager()
                ? all.stream().filter(q -> q.getStatus() == QuestionStatus.TRANSFERRED).toList()
                : List.of();
        model.addAttribute("transferred", transferred);
        long pendingCount = viewer.isManager()
                ? answers.findByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus.PENDING).stream()
                        .filter(answer -> ScopeService.allows(allowedManagerDepartments,
                                answer.getQuestion().getDepartment())).count()
                : 0;
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("facultyCount", faculties.count());
        model.addAttribute("faculties", viewer.isManager() ? visibleFaculties : List.of());
        model.addAttribute("selectedFacultyId", selectedFacultyId);
        if (viewer.isStudent()) {
            List<Faculty> allFaculties = faculties.findAll();
            List<FacultyTopic> allTopics = topics.findAllByOrderByFacultyIdAscIdAsc();
            List<AdvisorPresenceView> consultants = advisorProfiles.findAll().stream()
                    .filter(profile -> !profile.isDeleted() && profile.getStatus() == AdvisorStatus.ACTIVE)
                    .filter(profile -> profile.getDepartment() != null && profile.getUser() != null)
                    .filter(profile -> profile.getUser().getRole() == Role.CONSULTANT
                            && profile.getUser().isEnabled() && profile.getUser().getFaculty() != null)
                    .filter(profile -> profile.getUser().getFaculty().getName()
                            .equalsIgnoreCase(profile.getDepartment().getName()))
                    .map(profile -> new AdvisorPresenceView(profile.getUser().getFaculty().getId(),
                            profile.getUser().getDisplayName(), consultantTitle(profile),
                            advisorPresence.isOnline(profile.getUser().getId())))
                    .toList();
            List<UnitDirectoryView> unitDirectory = allFaculties.stream()
                    .map(faculty -> new UnitDirectoryView(faculty,
                            allTopics.stream().filter(topic -> topic.getFaculty().getId().equals(faculty.getId())).toList(),
                            consultants.stream().filter(consultant -> consultant.facultyId().equals(faculty.getId())).toList()))
                    .toList();
            model.addAttribute("unitDirectory", unitDirectory);
        }
        return "questions";
    }

    @GetMapping("/questions/new") public String newQuestion(Model model, HttpSession session) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (viewer == null) return "redirect:/login";
        if (!viewer.isStudent()) return "redirect:/questions";
        model.addAttribute("faculties", faculties.findAll());
        model.addAttribute("topics", topics.findAllByOrderByFacultyIdAscIdAsc()); return "question-new";
    }

    @PostMapping("/questions") public String createQuestion(@RequestParam String body,
            @RequestParam Long facultyId, @RequestParam Long topicId, @RequestParam(required = false) MultipartFile attachment,
            HttpSession session, RedirectAttributes flash) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (viewer == null) return "redirect:/login";
        if (!viewer.isStudent()) return "redirect:/questions";
        FacultyTopic topic = topics.findById(topicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Faculty faculty = faculties.findById(facultyId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!topic.getFaculty().getId().equals(faculty.getId())) {
            flash.addFlashAttribute("error", "Chuyên đề không thuộc khoa hoặc đơn vị đã chọn."); return "redirect:/questions/new";
        }
        if (body.isBlank()) {
            flash.addFlashAttribute("error", "Vui lòng nhập nội dung câu hỏi."); return "redirect:/questions/new";
        }
        String subject = body.trim().replaceAll("\\s+", " ");
        if (subject.length() > 240) subject = subject.substring(0, 237) + "...";
        Question question = new Question(viewer.user(), faculty, subject, body.trim(), LocalDateTime.now());
        question.setTopic(topic);
        question.setDepartment(departmentFor(faculty));
        question = questions.save(question);
        if (attachment != null && !attachment.isEmpty()) {
            try { question.setAttachmentUrl(cloudinary.upload(attachment)); questions.save(question); }
            catch (Exception ex) { flash.addFlashAttribute("warning", ex.getMessage()); }
        }
        flash.addFlashAttribute("success", "Câu hỏi đã được gửi đến " + faculty.getName() + ".");
        return "redirect:/questions/" + question.getId();
    }

    @GetMapping("/questions/public") public String publicQuestions(@RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long topicId, HttpSession session, Model model) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (viewer == null) return "redirect:/login";
        Faculty selectedFaculty = null;
        if (viewer.isStudent()) {
            selectedFaculty = facultyId == null ? null : faculties.findById(facultyId).orElse(null);
        } else if (viewer.role() == Role.ADMIN) {
            selectedFaculty = facultyId == null ? null : faculties.findById(facultyId).orElse(null);
        } else {
            selectedFaculty = viewer.user().getFaculty();
        }
        FacultyTopic selectedTopic = topicId == null ? null : topics.findById(topicId).orElse(null);
        if (selectedTopic != null && selectedFaculty == null) {
            if (viewer.isStudent()) selectedFaculty = selectedTopic.getFaculty();
            else selectedTopic = null;
        }
        if (selectedTopic != null && selectedFaculty != null
                && !selectedTopic.getFaculty().getId().equals(selectedFaculty.getId())) {
            selectedTopic = null;
        }
        List<Question> publicQuestions = selectedFaculty == null
                ? viewer.isStudent() || viewer.role() == Role.ADMIN
                        ? questions.findByIsPublicTrueOrderByCreatedAtDesc() : List.of()
                : questions.findByFacultyIdAndIsPublicTrueOrderByCreatedAtDesc(selectedFaculty.getId());
        if (selectedTopic != null) {
            Long selectedId = selectedTopic.getId();
            publicQuestions = publicQuestions.stream().filter(question -> question.getTopic() != null
                    && question.getTopic().getId().equals(selectedId)).toList();
        }
        if (viewer.isAdvisor()) publicQuestions = publicQuestions.stream().filter(question -> canHandle(viewer, question)).toList();
        model.addAttribute("publicQuestions", publicQuestions);
        model.addAttribute("publicFaculties", faculties.findAll());
        model.addAttribute("selectedFaculty", selectedFaculty);
        model.addAttribute("selectedTopic", selectedTopic);
        model.addAttribute("publicTopics", selectedFaculty == null ? List.of() : topics.findByFacultyIdOrderByIdAsc(selectedFaculty.getId()));
        return "public-questions";
    }

    public record AdvisorPresenceView(Long facultyId, String name, String title, boolean online) {}
    public record UnitDirectoryView(Faculty faculty, List<FacultyTopic> topics, List<AdvisorPresenceView> consultants) {}

    private String consultantTitle(AdvisorProfile profile) {
        if (profile.getAdvisorType() != null) return profile.getAdvisorType().getLabel();
        return profile.getUser().getConsultantTitles().stream()
                .filter(title -> title != AdvisorType.STUDENT)
                .map(AdvisorType::getLabel).findFirst().orElse(AdvisorType.ADVISOR.getLabel());
    }

    @GetMapping("/questions/{id}") public String detail(@PathVariable Long id, HttpSession session, Model model) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (viewer == null) return "redirect:/login";
        Question question = questions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureCanView(viewer, question);
        List<Answer> visibleAnswers = answers.findByQuestionIdOrderByCreatedAtAsc(id);
        if (viewer.isStudent()) visibleAnswers = visibleAnswers.stream().filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED || a.getApprovalStatus() == ApprovalStatus.NOT_REQUIRED).toList();
        model.addAttribute("question", question); model.addAttribute("answers", visibleAnswers);
        model.addAttribute("hideAskerIdentity", viewer.isStudent() && !question.getAsker().getId().equals(viewer.user().getId()));
        model.addAttribute("faculties", faculties.findAll());
        model.addAttribute("topics", topics.findAllByOrderByFacultyIdAscIdAsc());
        boolean managerScope = viewer.isManager() && managerCanAccess(viewer, question);
        boolean canManage = managerScope || (viewer.isAdvisor() && canHandle(viewer, question));
        model.addAttribute("canManageQuestion", canManage && !question.isLocked());
        model.addAttribute("canAnswerQuestion", managerScope || (!question.isLocked()
                && viewer.isAdvisor() && canHandle(viewer, question)));
        return "question-detail";
    }

    @PostMapping("/questions/{id}/answers") public String reply(@PathVariable Long id, @RequestParam String body,
            @RequestParam(defaultValue = "false") boolean requestApproval, HttpSession session, RedirectAttributes flash) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (viewer == null) return "redirect:/login";
        Question question = questions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureCanAnswer(viewer, question);
        if (question.isLocked() && !viewer.isManager()) { flash.addFlashAttribute("error", "Câu hỏi đã khóa do chuyển khoa, không thể trả lời."); return "redirect:/questions/" + id; }
        if (body.isBlank()) { flash.addFlashAttribute("error", "Nội dung trả lời không được để trống."); return "redirect:/questions/" + id; }
        boolean approvalRequired = viewer.isAdvisor() && requestApproval;
        ApprovalStatus status = approvalRequired ? ApprovalStatus.PENDING : ApprovalStatus.NOT_REQUIRED;
        answers.save(new Answer(question, viewer.user(), body.trim(), LocalDateTime.now(), status));
        if (approvalRequired) {
            notifications.notifyManagers(question.getFaculty().getId(), "approval", "Có câu trả lời cần trưởng ban cố vấn duyệt.", question.getId());
            flash.addFlashAttribute("success", "Đã gửi câu trả lời để quản lý duyệt.");
        } else {
            question.setStatus(QuestionStatus.ANSWERED); question.setReplyNotification(true);
            if (viewer.isManager()) question.setLocked(false);
            questions.save(question);
            notifications.notifyUser(question.getAsker().getId(), "reply", "Câu hỏi của bạn vừa có tư vấn viên phản hồi.", question.getId());
            flash.addFlashAttribute("success", "Đã gửi câu trả lời và thông báo cho người hỏi.");
        }
        return "redirect:/questions/" + id;
    }

    @PostMapping("/questions/{id}/visibility") public String toggleVisibility(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (viewer == null) return "redirect:/login";
        Question question = questions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureCanManage(viewer, question);
        question.setPublic(!question.isPublic()); questions.save(question);
        flash.addFlashAttribute("success", question.isPublic() ? "Câu hỏi đã được công khai." : "Câu hỏi đã chuyển về chế độ riêng tư.");
        return "redirect:/questions/" + id;
    }

    @PostMapping("/questions/{id}/transfer") public String transfer(@PathVariable Long id, @RequestParam Long facultyId, @RequestParam Long topicId,
            @RequestParam String reason, HttpSession session, RedirectAttributes flash) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (viewer == null) return "redirect:/login";
        Question question = questions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureCanManage(viewer, question);
        Faculty target = faculties.findById(facultyId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        FacultyTopic targetTopic = topics.findById(topicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (question.isLocked()) { flash.addFlashAttribute("error", "Câu hỏi này đã được khóa."); return "redirect:/questions/" + id; }
        if (target.getId().equals(question.getFaculty().getId())) { flash.addFlashAttribute("error", "Hãy chọn một khoa khác."); return "redirect:/questions/" + id; }
        if (!targetTopic.getFaculty().getId().equals(target.getId())) { flash.addFlashAttribute("error", "Chuyên đề không thuộc đơn vị đã chọn."); return "redirect:/questions/" + id; }
        if (reason.isBlank()) { flash.addFlashAttribute("error", "Vui lòng ghi lý do chuyển khoa."); return "redirect:/questions/" + id; }
        Faculty source = question.getFaculty();
        transfers.save(new QuestionTransfer(question, source, target, viewer.user(), reason.trim(), LocalDateTime.now()));
        question.setTopic(targetTopic); question.setDepartment(departmentFor(target));
        question.setLocked(true); question.setStatus(QuestionStatus.TRANSFERRED); questions.save(question);
        notifications.notifyManagers(target.getId(), "transfer", "Một câu hỏi mới được chuyển đến khoa của bạn.", question.getId());
        flash.addFlashAttribute("success", "Đã chuyển câu hỏi đến " + target.getName() + ". Câu hỏi đã khóa và không thể trả lời.");
        return "redirect:/questions";
    }

    private void ensureCanView(SessionContext.Viewer viewer, Question question) {
        boolean owner = question.getAsker().getId().equals(viewer.user().getId());
        if (viewer.role() == Role.ADMIN) return;
        if (viewer.isManager()) {
            if (!managerCanAccess(viewer, question)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            return;
        }
        if (viewer.isAdvisor()) {
            if (!canHandle(viewer, question)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } else if (!owner && !question.isPublic()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private vn.iotstar.entity.Department departmentFor(Faculty faculty) {
        return departments.findAll().stream()
                .filter(department -> department.getName().trim().equalsIgnoreCase(faculty.getName().trim()))
                .findFirst().orElseGet(() -> departments.save(new vn.iotstar.entity.Department(faculty.getName().trim())));
    }
    private void ensureCanManage(SessionContext.Viewer viewer, Question question) {
        if (viewer.isManager()) {
            if (!managerCanAccess(viewer, question)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            return;
        }
        if (!viewer.isAdvisor() || !canHandle(viewer, question))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private void ensureCanAnswer(SessionContext.Viewer viewer, Question question) {
        if (viewer.isManager()) {
            if (!managerCanAccess(viewer, question)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            return;
        }
        if (!viewer.isAdvisor() || !canHandle(viewer, question))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private boolean canHandle(SessionContext.Viewer viewer, Question question) {
        if (viewer.user().getFaculty() == null || question.getFaculty() == null
                || !viewer.user().getFaculty().getId().equals(question.getFaculty().getId())) return false;
        return viewer.isManager() || (viewer.isAdvisor() && viewer.user().isAssignedTo(question.getTopic()));
    }

    private boolean managerCanAccess(SessionContext.Viewer viewer, Question question) {
        return ScopeService.allows(scope.managerDepartments(viewer.user().getEmail()), question.getDepartment());
    }
}
