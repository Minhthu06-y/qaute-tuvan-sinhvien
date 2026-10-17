package vn.iotstar.user.service;

import vn.iotstar.user.dto.QuestionRequest;
import vn.iotstar.user.entity.Question;
import vn.iotstar.user.entity.User;
import vn.iotstar.user.repository.QuestionRepository;
import vn.iotstar.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    public QuestionService(
            QuestionRepository questionRepository,
            UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Question> getPublicQuestions() {
        return questionRepository
            .findByIsPublicTrueAndLockedFalseOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Question> getMyQuestions(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException(
                "Không tìm thấy tài khoản"));

        return questionRepository
            .findByAuthor_IdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public void createQuestion(String email, QuestionRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException(
                "Không tìm thấy tài khoản"));

        if (!user.isEnabled() || !user.isActive()) {
            throw new IllegalStateException(
                "Tài khoản chưa xác minh hoặc đã bị khóa");
        }

        Question question = new Question(
            request.getTitle().trim(),
            request.getContent().trim(),
            user
        );

        // Mặc định không công khai cho đến khi được xử lý.
        question.setPublic(false);
        questionRepository.save(question);
    }
}