package vn.iotstar.service;

import vn.iotstar.entity.*;
import vn.iotstar.enums.*;
import vn.iotstar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionBrowseService {
    private final QauteQuestionRepository questions;
    private final QauteAnswerRepository answers;
    private final UserRepository users;
    private final NotificationService notifications;
    private final ActivityLogService logs;

    /** allowed: null = tất cả (Admin); rỗng = không khoa nào. */
    public Page<Question> search(QuestionStatus status, Long deptId, String category, String kw,
                                 Set<Long> allowed, int page, int size) {
        String k = kw == null ? "" : kw.trim().toLowerCase();
        String selectedCategory = category == null ? "" : category.trim();
        List<Question> all = questions.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(q -> ScopeService.allows(allowed, q.getDepartment()))
                .filter(q -> status == null || q.getStatus() == status)
                .filter(q -> deptId == null || (q.getDepartment() != null && q.getDepartment().getId().equals(deptId)))
                .filter(q -> selectedCategory.isEmpty() || (q.getCategory() != null && q.getCategory().equalsIgnoreCase(selectedCategory)))
                .filter(q -> k.isEmpty()
                        || (q.getTitle() != null && q.getTitle().toLowerCase().contains(k))
                        || (q.getAsker() != null && q.getAsker().getFullName() != null && q.getAsker().getFullName().toLowerCase().contains(k)))
                .toList();
        int from = Math.min(page * size, all.size()), to = Math.min(from + size, all.size());
        return new PageImpl<>(all.subList(from, to), PageRequest.of(page, size), all.size());
    }

    public List<String> categories(Set<Long> allowed) {
        return questions.findAll().stream()
                .filter(q -> ScopeService.allows(allowed, q.getDepartment()))
                .map(Question::getCategory).filter(category -> category != null && !category.isBlank())
                .map(String::trim).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    /** Số câu theo từng trạng thái (hiện trên tab). */
    public Map<String, Long> counts(Set<Long> allowed) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (QuestionStatus s : QuestionStatus.values()) m.put(s.name(), 0L);
        questions.findAll().stream().filter(q -> ScopeService.allows(allowed, q.getDepartment()))
                .forEach(q -> m.merge(q.getStatus().name(), 1L, Long::sum));
        return m;
    }

    public Question get(Long id, Set<Long> allowed) {
        Question q = questions.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi"));
        if (!ScopeService.allows(allowed, q.getDepartment()))
            throw new AccessDeniedException("Câu hỏi không thuộc khoa bạn phụ trách");
        return q;
    }

    public List<Answer> thread(Long questionId) { return answers.findByQuestion_IdOrderByCreatedAtAsc(questionId); }

    @Transactional
    public void answer(Long questionId, String content, String managerEmail) {
        Question question = get(questionId, null);
        User manager = users.findByEmailIgnoreCase(managerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản trưởng ban cố vấn"));
        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setAdvisor(manager);
        answer.setContent(content.trim());
        answer.setNeedApproval(false);
        answer.setApprovalStatus(ApprovalStatus.APPROVED);
        answer.setPublished(true);
        answer.setApproved(true);
        answers.save(answer);

        question.setStatus(QuestionStatus.ANSWERED);
        questions.save(question);
        if (question.getAsker() != null) {
            notifications.notify(question.getAsker(), "Trưởng ban cố vấn đã trả lời câu hỏi: " + question.getTitle(), "/");
        }
        logs.log(managerEmail, "MANAGER", "ANSWER_QUESTION", "question#" + questionId);
    }
}

