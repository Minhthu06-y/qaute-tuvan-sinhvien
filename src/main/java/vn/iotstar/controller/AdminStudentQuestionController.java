package vn.iotstar.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.time.LocalDateTime;
import vn.iotstar.dto.AdminQuestionForm;
import vn.iotstar.entity.Department;
import vn.iotstar.enums.Role;
import vn.iotstar.model.FacultyTopic;
import vn.iotstar.repository.DepartmentRepository;
import vn.iotstar.repository.FacultyTopicRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.QuestionModerationService;
import vn.iotstar.model.Answer;
import vn.iotstar.model.Faculty;
import vn.iotstar.model.Question;
import vn.iotstar.enums.*;
import vn.iotstar.repository.AnswerRepository;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.QuestionRepository;

@Controller
@RequestMapping("/admin/student-questions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudentQuestionController {
    private static final int PAGE_SIZE = 15;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final FacultyRepository faculties;
    private final FacultyTopicRepository topics;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final QuestionModerationService moderation;

    public AdminStudentQuestionController(QuestionRepository questions, AnswerRepository answers,
            FacultyRepository faculties, FacultyTopicRepository topics, UserRepository users,
            DepartmentRepository departments, QuestionModerationService moderation) {
        this.questions = questions;
        this.answers = answers;
        this.faculties = faculties;
        this.topics = topics;
        this.users = users;
        this.departments = departments;
        this.moderation = moderation;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", new AdminQuestionForm());
        return formModel(model);
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") AdminQuestionForm form, BindingResult binding,
            Model model, Principal principal, RedirectAttributes flash) {
        if (!binding.hasErrors()) {
            try {
                vn.iotstar.entity.User asker = users.findById(form.getAskerId())
                        .filter(user -> user.getRole() == Role.USER && !user.isDeleted())
                        .orElseThrow(() -> new IllegalArgumentException("Chỉ chọn tài khoản người dùng đang hoạt động"));
                vn.iotstar.model.Faculty faculty = faculties.findById(form.getFacultyId())
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khoa"));
                FacultyTopic topic = topics.findById(form.getTopicId())
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chuyên đề"));
                if (!topic.getFaculty().getId().equals(faculty.getId()))
                    throw new IllegalArgumentException("Chuyên đề không thuộc khoa đã chọn");
                String subject = form.getSubject().trim();
                Question question = new Question(asker, faculty, subject, form.getBody().trim(), LocalDateTime.now());
                question.setTopic(topic);
                question.setDepartment(departmentFor(faculty));
                question.setPublic(form.isPublicQuestion());
                question.setStatus(QuestionStatus.UNANSWERED);
                questions.save(question);
                flash.addFlashAttribute("msg", "Đã thêm câu hỏi");
                return "redirect:/admin/student-questions/" + question.getId();
            } catch (IllegalArgumentException error) {
                binding.reject("custom", error.getMessage());
            }
        }
        return formModel(model);
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "ALL") String status, @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) String kw, @RequestParam(defaultValue = "0") int page, Model model) {
        QuestionStatus selectedStatus = parseStatus(status);
        String search = kw == null ? "" : kw.trim().toLowerCase(Locale.ROOT);
        List<Question> filtered = questions.findAll().stream()
                .filter(question -> !question.isDeleted())
                .filter(question -> selectedStatus == null || matchesStatus(question.getStatus(), selectedStatus))
                .filter(question -> facultyId == null || (question.getFaculty() != null && question.getFaculty().getId().equals(facultyId)))
                .filter(question -> search.isEmpty() || matches(question, search))
                .sorted(Comparator.comparing(Question::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * PAGE_SIZE, filtered.size());
        int to = Math.min(from + PAGE_SIZE, filtered.size());
        Page<Question> result = new PageImpl<>(filtered.subList(from, to), PageRequest.of(safePage, PAGE_SIZE), filtered.size());
        model.addAttribute("page", result);
        model.addAttribute("faculties", faculties.findAll());
        model.addAttribute("status", selectedStatus == null ? "ALL" : selectedStatus.name());
        model.addAttribute("facultyId", facultyId);
        model.addAttribute("kw", kw);
        model.addAttribute("openCount", questions.findAll().stream().filter(q -> !q.isDeleted()).filter(q -> q.getStatus() == QuestionStatus.OPEN
                || q.getStatus() == QuestionStatus.UNANSWERED).count());
        model.addAttribute("answeredCount", questions.findAll().stream().filter(q -> !q.isDeleted()).filter(q -> q.getStatus() == QuestionStatus.ANSWERED).count());
        model.addAttribute("totalCount", questions.findAll().stream().filter(q -> !q.isDeleted()).count());
        return "admin/student-questions";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Question question = questions.findById(id).filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Answer> thread = answers.findByQuestionIdOrderByCreatedAtAsc(id);
        model.addAttribute("question", question);
        model.addAttribute("answers", thread);
        return "admin/student-question-detail";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        moderation.deleteByAdmin(id, principal.getName());
        flash.addFlashAttribute("msg", "Đã gỡ câu hỏi khỏi danh sách hiển thị.");
        return "redirect:/admin/student-questions";
    }

    private String formModel(Model model) {
        model.addAttribute("users", users.findByRole(Role.USER).stream().filter(user -> !user.isDeleted()).toList());
        model.addAttribute("faculties", faculties.findAll());
        model.addAttribute("topics", topics.findAllByOrderByFacultyIdAscIdAsc());
        return "admin/student-question-form";
    }

    private Department departmentFor(vn.iotstar.model.Faculty faculty) {
        return departments.findAll().stream()
                .filter(department -> department.getName().trim().equalsIgnoreCase(faculty.getName().trim()))
                .findFirst().orElseGet(() -> departments.save(new Department(faculty.getName().trim())));
    }

    private QuestionStatus parseStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) return null;
        try {
            return QuestionStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean matchesStatus(QuestionStatus actual, QuestionStatus selected) {
        if (selected == QuestionStatus.OPEN || selected == QuestionStatus.UNANSWERED)
            return actual == QuestionStatus.OPEN || actual == QuestionStatus.UNANSWERED;
        return actual == selected;
    }

    private boolean matches(Question question, String search) {
        return contains(question.getSubject(), search)
                || contains(question.getBody(), search)
                || (question.getAsker() != null && (contains(question.getAsker().getDisplayName(), search)
                        || contains(question.getAsker().getEmail(), search)));
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }
}
