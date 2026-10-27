package vn.iotstar.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.dto.QuestionRequest;
import vn.iotstar.entity.Question;
import vn.iotstar.entity.User;
import vn.iotstar.enums.*;
import vn.iotstar.repository.QauteQuestionRepository;
import vn.iotstar.repository.UserRepository;

@Service
public class QuestionService {

    private final QauteQuestionRepository questionRepository;
    private final UserRepository userRepository;

    public QuestionService(
            QauteQuestionRepository questionRepository,
            UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Question> getPublicQuestions() {
        return questionRepository
            .findByPublicVisibleTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Question> getMyQuestions(String email) {
        User user = userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException(
                "Không tìm thấy tài khoản."
            ));

        return questionRepository
            .findByAsker_IdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public void createQuestion(
            String email, QuestionRequest request) {

        User user = userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalArgumentException(
                "Không tìm thấy tài khoản."
            ));

        if (!user.isEnabled()
                || user.getStatus()
                    != vn.iotstar.enums.UserStatus.ACTIVE) {
            throw new IllegalStateException(
                "Tài khoản không hoạt động."
            );
        }

        Question question = new Question();
        question.setTitle(request.getTitle().trim());
        question.setContent(request.getContent().trim());
        question.setCategory(request.getCategory().trim());
        question.setAsker(user);

        // Chờ quy trình xét duyệt trước khi công khai.
        question.setPublicQuestion(false);
        question.setStatus(QuestionStatus.UNANSWERED);

        questionRepository.save(question);
    }
}
