package vn.iotstar.controller;

import java.time.LocalDateTime;
import java.util.List;
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
import vn.iotstar.model.*;
import vn.iotstar.repository.*;
import vn.iotstar.service.CloudinaryStorageService;
import vn.iotstar.service.NotificationService;
import vn.iotstar.service.SessionContext;

@Controller
public class QuestionController {
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final FacultyRepository faculties;
    private final FacultyTopicRepository topics;
    private final QuestionTransferRepository transfers;
    private final SessionContext sessionContext;
    private final NotificationService notifications;
    private final CloudinaryStorageService cloudinary;

    public QuestionController(QuestionRepository questions, AnswerRepository answers, FacultyRepository faculties, FacultyTopicRepository topics,
            QuestionTransferRepository transfers, SessionContext sessionContext, NotificationService notifications,
            CloudinaryStorageService cloudinary) {
        this.questions = questions; this.answers = answers; this.faculties = faculties; this.topics = topics; this.transfers = transfers;
        this.sessionContext = sessionContext; this.notifications = notifications; this.cloudinary = cloudinary;
    }

    @GetMapping({"/", "/questions"}) public String dashboard(@RequestParam(required = false) Long topicId, HttpSession session, Model model) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        List<Question> all;
        if (viewer.isStudent()) all = questions.findByAskerIdOrderByCreatedAtDesc(viewer.user().getId());
        else {
            all = questions.findByFacultyIdAndLockedFalseOrderByCreatedAtDesc(viewer.user().getFaculty().getId());
            if (!viewer.isManager()) all = all.stream().filter(question -> canHandle(viewer, question)).toList();
        }
        List<FacultyTopic> availableTopics = viewer.isStudent() ? List.of()
                : viewer.isManager() ? topics.findByFacultyIdOrderByIdAsc(viewer.user().getFaculty().getId())
                : viewer.user().getAssignedTopics().stream()
                        .filter(topic -> topic.getFaculty().getId().equals(viewer.user().getFaculty().getId()))
                        .sorted(java.util.Comparator.comparing(FacultyTopic::getId)).toList();
        Long selectedTopicId = null;
        if (topicId != null && availableTopics.stream().anyMatch(topic -> topic.getId().equals(topicId))) {
            selectedTopicId = topicId;
            all = all.stream().filter(question -> question.getTopic() != null && question.getTopic().getId().equals(topicId)).toList();
        }
        List<Question> unanswered = all.stream().filter(q -> q.getStatus() == QuestionStatus.OPEN).toList();
        List<Question> answered = all.stream().filter(q -> q.getStatus() == QuestionStatus.ANSWERED).toList();
        model.addAttribute("unanswered", unanswered);
        model.addAttribute("answered", answered);
        model.addAttribute("topics", availableTopics);
        model.addAttribute("selectedTopicId", selectedTopicId);
        List<Question> transferred = viewer.isManager()
                ? questions.findByFacultyIdOrderByCreatedAtDesc(viewer.user().getFaculty().getId()).stream().filter(q -> q.getStatus() == QuestionStatus.TRANSFERRED).toList()
                : List.of();
        model.addAttribute("transferred", transferred);
        model.addAttribute("pendingCount", viewer.isManager() ? answers.findByApprovalStatusAndQuestionFacultyIdOrderByCreatedAtAsc(ApprovalStatus.PENDING, viewer.user().getFaculty().getId()).size() : 0);
        model.addAttribute("facultyCount", faculties.count());
        return "questions";
    }

    @GetMapping("/questions/new") public String newQuestion(Model model, HttpSession session) {
        if (!sessionContext.viewer(session).isStudent()) return "redirect:/questions";
        model.addAttribute("faculties", faculties.findAll());
        model.addAttribute("topics", topics.findAllByOrderByFacultyIdAscIdAsc()); return "question-new";
    }

    @PostMapping("/questions") public String createQuestion(@RequestParam String body,
            @RequestParam Long facultyId, @RequestParam Long topicId, @RequestParam(required = false) MultipartFile attachment,
            HttpSession session, RedirectAttributes flash) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
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
        question = questions.save(question);
        if (attachment != null && !attachment.isEmpty()) {
            try { question.setAttachmentUrl(cloudinary.upload(attachment)); questions.save(question); }
            catch (Exception ex) { flash.addFlashAttribute("warning", ex.getMessage()); }
        }
        flash.addFlashAttribute("success", "Câu hỏi đã được gửi đến " + faculty.getName() + ".");
        return "redirect:/questions/" + question.getId();
    }

    @GetMapping("/questions/public") public String publicQuestions(HttpSession session, Model model) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        if (viewer.user().getFaculty() == null) model.addAttribute("publicQuestions", List.of());
        else {
            List<Question> publicQuestions = questions.findByFacultyIdAndIsPublicTrueOrderByCreatedAtDesc(viewer.user().getFaculty().getId());
            if (viewer.isAdvisor()) publicQuestions = publicQuestions.stream().filter(question -> canHandle(viewer, question)).toList();
            model.addAttribute("publicQuestions", publicQuestions);
        }
        return "public-questions";
    }

    @GetMapping("/questions/{id}") public String detail(@PathVariable Long id, HttpSession session, Model model) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        Question question = questions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureCanView(viewer, question);
        List<Answer> visibleAnswers = answers.findByQuestionIdOrderByCreatedAtAsc(id);
        if (viewer.isStudent()) visibleAnswers = visibleAnswers.stream().filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED || a.getApprovalStatus() == ApprovalStatus.NOT_REQUIRED).toList();
        model.addAttribute("question", question); model.addAttribute("answers", visibleAnswers);
        model.addAttribute("hideAskerIdentity", viewer.isStudent() && !question.getAsker().getId().equals(viewer.user().getId()));
        model.addAttribute("faculties", faculties.findAll());
        model.addAttribute("topics", topics.findAllByOrderByFacultyIdAscIdAsc());
        model.addAttribute("canManageQuestion", (viewer.isAdvisor() || viewer.isManager()) && canHandle(viewer, question) && !question.isLocked());
        return "question-detail";
    }

    @PostMapping("/questions/{id}/answers") public String reply(@PathVariable Long id, @RequestParam String body,
            @RequestParam(defaultValue = "false") boolean requestApproval, HttpSession session, RedirectAttributes flash) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        Question question = questions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureCanManage(viewer, question);
        if (question.isLocked()) { flash.addFlashAttribute("error", "Câu hỏi đã khóa do chuyển khoa, không thể trả lời."); return "redirect:/questions/" + id; }
        if (body.isBlank()) { flash.addFlashAttribute("error", "Nội dung trả lời không được để trống."); return "redirect:/questions/" + id; }
        ApprovalStatus status = requestApproval ? ApprovalStatus.PENDING : ApprovalStatus.NOT_REQUIRED;
        answers.save(new Answer(question, viewer.user(), body.trim(), LocalDateTime.now(), status));
        if (requestApproval) {
            notifications.notifyManagers(question.getFaculty().getId(), "approval", "Có câu trả lời cần trưởng nhóm duyệt.", question.getId());
            flash.addFlashAttribute("success", "Đã gửi câu trả lời để quản lý duyệt.");
        } else {
            question.setStatus(QuestionStatus.ANSWERED); question.setReplyNotification(true); questions.save(question);
            notifications.notifyUser(question.getAsker().getId(), "reply", "Câu hỏi của bạn vừa có tư vấn viên phản hồi.", question.getId());
            flash.addFlashAttribute("success", "Đã gửi câu trả lời và thông báo cho người hỏi.");
        }
        return "redirect:/questions/" + id;
    }

    @PostMapping("/questions/{id}/visibility") public String toggleVisibility(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
        Question question = questions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ensureCanManage(viewer, question);
        question.setPublic(!question.isPublic()); questions.save(question);
        flash.addFlashAttribute("success", question.isPublic() ? "Câu hỏi đã được công khai." : "Câu hỏi đã chuyển về chế độ riêng tư.");
        return "redirect:/questions/" + id;
    }

    @PostMapping("/questions/{id}/transfer") public String transfer(@PathVariable Long id, @RequestParam Long facultyId, @RequestParam Long topicId,
            @RequestParam String reason, HttpSession session, RedirectAttributes flash) {
        SessionContext.Viewer viewer = sessionContext.viewer(session);
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
        question.setTopic(targetTopic); question.setLocked(true); question.setStatus(QuestionStatus.TRANSFERRED); questions.save(question);
        notifications.notifyManagers(target.getId(), "transfer", "Một câu hỏi mới được chuyển đến khoa của bạn.", question.getId());
        flash.addFlashAttribute("success", "Đã chuyển câu hỏi đến " + target.getName() + ". Câu hỏi đã khóa và không thể trả lời.");
        return "redirect:/questions";
    }

    private void ensureCanView(SessionContext.Viewer viewer, Question question) {
        boolean owner = question.getAsker().getId().equals(viewer.user().getId());
        if (viewer.isManager() || viewer.isAdvisor()) {
            if (!canHandle(viewer, question)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } else if (!owner && !question.isPublic()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
    private void ensureCanManage(SessionContext.Viewer viewer, Question question) {
        if (!(viewer.isAdvisor() || viewer.isManager()) || !canHandle(viewer, question))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    private boolean canHandle(SessionContext.Viewer viewer, Question question) {
        if (viewer.user().getFaculty() == null || question.getFaculty() == null
                || !viewer.user().getFaculty().getId().equals(question.getFaculty().getId())) return false;
        return viewer.isManager() || (viewer.isAdvisor() && viewer.user().isAssignedTo(question.getTopic()));
    }
}
