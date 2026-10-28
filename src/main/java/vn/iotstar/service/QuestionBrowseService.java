package vn.iotstar.service;

import vn.iotstar.entity.User;
import vn.iotstar.enums.ApprovalStatus;
import vn.iotstar.enums.QuestionStatus;
import vn.iotstar.model.Answer;
import vn.iotstar.model.Question;
import vn.iotstar.repository.AnswerRepository;
import vn.iotstar.repository.QuestionRepository;
import vn.iotstar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionBrowseService {
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final UserRepository users;
    private final NotificationService notifications;
    private final ActivityLogService logs;

    public Page<Question> search(QuestionStatus status, Long deptId, String category, String kw,
                                 Set<Long> allowed, int page, int size) {
        String keyword = kw == null ? "" : kw.trim().toLowerCase(Locale.ROOT);
        String selectedCategory = category == null ? "" : category.trim();
        List<Question> all = questions.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(question -> !question.isDeleted())
                .filter(question -> ScopeService.allows(allowed, question.getDepartment()))
                .filter(question -> status == null || matchesStatus(question.getStatus(), status))
                .filter(question -> deptId == null || (question.getDepartment() != null
                        && question.getDepartment().getId().equals(deptId)))
                .filter(question -> selectedCategory.isEmpty() || (question.getCategory() != null
                        && question.getCategory().equalsIgnoreCase(selectedCategory)))
                .filter(question -> keyword.isEmpty()
                        || (question.getTitle() != null && question.getTitle().toLowerCase(Locale.ROOT).contains(keyword))
                        || (question.getAsker() != null && question.getAsker().getFullName() != null
                                && question.getAsker().getFullName().toLowerCase(Locale.ROOT).contains(keyword)))
                .toList();
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageImpl<>(all.subList(from, to), PageRequest.of(page, size), all.size());
    }

    public List<String> categories(Set<Long> allowed) {
        return questions.findAll().stream()
                .filter(question -> !question.isDeleted())
                .filter(question -> ScopeService.allows(allowed, question.getDepartment()))
                .map(Question::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public Map<String, Long> counts(Set<Long> allowed) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (QuestionStatus status : QuestionStatus.values()) counts.put(status.name(), 0L);
        questions.findAll().stream()
                .filter(question -> !question.isDeleted())
                .filter(question -> ScopeService.allows(allowed, question.getDepartment()))
                .forEach(question -> counts.merge(normalizeStatus(question.getStatus()).name(), 1L, Long::sum));
        return counts;
    }

    public Question get(Long id, Set<Long> allowed) {
        Question question = questions.findById(id)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi"));
        if (!ScopeService.allows(allowed, question.getDepartment()))
            throw new AccessDeniedException("Câu hỏi không thuộc khoa bạn phụ trách");
        return question;
    }

    public List<Answer> thread(Long questionId) {
        return answers.findByQuestionIdOrderByCreatedAtAsc(questionId);
    }

    public Answer answer(Long answerId) {
        return answers.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu trả lời"));
    }

    @Transactional
    public void editManagerAnswer(Long answerId, String content, String managerEmail, Set<Long> allowed) {
        if (content == null || content.isBlank() || content.trim().length() > 10000)
            throw new IllegalArgumentException("Nội dung bình luận phải có từ 1 đến 10.000 ký tự");
        Answer answer = answers.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu trả lời"));
        Question question = get(answer.getQuestion().getId(), allowed);
        User manager = users.findByEmailIgnoreCase(managerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản Manager"));
        if (!answer.getAuthor().getId().equals(manager.getId()))
            throw new AccessDeniedException("Bạn chỉ được sửa bình luận do mình viết");
        answer.setBody(answer.withPreservedSeedMarker(content.trim()));
        answer.setEditedAt(LocalDateTime.now());
        answers.save(answer);
        if (question.getAsker() != null)
            notifications.notify(question.getAsker(), "Manager đã chỉnh sửa câu trả lời.", "/manager/questions/" + question.getId());
        logs.log(managerEmail, "MANAGER", "EDIT_ANSWER", "answer#" + answerId);
    }

    @Transactional
    public void answer(Long questionId, String content, String managerEmail, Set<Long> allowed) {
        Question question = get(questionId, allowed);
        User manager = users.findByEmailIgnoreCase(managerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản trưởng ban cố vấn"));
        Answer answer = new Answer(question, manager, content.trim(), LocalDateTime.now(), ApprovalStatus.NOT_REQUIRED);
        answers.save(answer);

        question.setStatus(QuestionStatus.ANSWERED);
        question.setReplyNotification(true);
        questions.save(question);
        if (question.getAsker() != null) {
            notifications.notify(question.getAsker(),
                    "Trưởng ban cố vấn đã trả lời câu hỏi: " + question.getTitle(), "/");
        }
        logs.log(managerEmail, "MANAGER", "ANSWER_QUESTION", "question#" + questionId);
    }

    private boolean matchesStatus(QuestionStatus actual, QuestionStatus requested) {
        return normalizeStatus(actual) == normalizeStatus(requested);
    }

    private QuestionStatus normalizeStatus(QuestionStatus status) {
        return status == QuestionStatus.OPEN ? QuestionStatus.UNANSWERED : status;
    }
}
