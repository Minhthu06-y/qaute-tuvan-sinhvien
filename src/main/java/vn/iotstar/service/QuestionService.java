package vn.iotstar.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.dto.QuestionRequest;
import vn.iotstar.entity.User;
import vn.iotstar.enums.QuestionStatus;
import vn.iotstar.model.Question;
import vn.iotstar.repository.DepartmentRepository;
import vn.iotstar.repository.QuestionRepository;
import vn.iotstar.repository.UserRepository;

@Service
public class QuestionService {
    private final QuestionRepository questions;
    private final UserRepository users;
    private final DepartmentRepository departments;

    public QuestionService(QuestionRepository questions, UserRepository users, DepartmentRepository departments) {
        this.questions = questions;
        this.users = users;
        this.departments = departments;
    }

    @Transactional(readOnly = true)
    public List<Question> getPublicQuestions() {
        return questions.findByIsPublicTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Question> getMyQuestions(String email) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        return questions.findByAskerIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public void createQuestion(String email, QuestionRequest request) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (!user.isEnabled() || user.getStatus() != vn.iotstar.enums.UserStatus.ACTIVE)
            throw new IllegalStateException("Tài khoản không hoạt động.");

        Question question = new Question(user, user.getFaculty(), request.getTitle().trim(),
                request.getContent().trim(), LocalDateTime.now());
        question.setCategory(request.getCategory().trim());
        question.setStatus(QuestionStatus.UNANSWERED);
        if (user.getFaculty() != null) {
            vn.iotstar.entity.Department matchedDepartment = departments.findAll().stream()
                    .filter(candidate -> candidate.getName().equalsIgnoreCase(user.getFaculty().getName()))
                    .findFirst()
                    .orElseGet(() -> departments.save(new vn.iotstar.entity.Department(user.getFaculty().getName())));
            question.setDepartment(matchedDepartment);
        }
        questions.save(question);
    }
}
