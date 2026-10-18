package vn.iotstar.qaute.service;

import vn.iotstar.qaute.entity.*;
import vn.iotstar.qaute.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionBrowseService {
    private final QuestionRepository questions;
    private final AnswerRepository answers;

    /** allowed: null = tất cả (Admin); rỗng = không khoa nào. */
    public Page<Question> search(QuestionStatus status, Long deptId, String kw, Set<Long> allowed, int page, int size) {
        String k = kw == null ? "" : kw.trim().toLowerCase();
        List<Question> all = questions.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(q -> ScopeService.allows(allowed, q.getDepartment()))
                .filter(q -> status == null || q.getStatus() == status)
                .filter(q -> deptId == null || (q.getDepartment() != null && q.getDepartment().getId().equals(deptId)))
                .filter(q -> k.isEmpty()
                        || (q.getTitle() != null && q.getTitle().toLowerCase().contains(k))
                        || (q.getAsker() != null && q.getAsker().getFullName() != null && q.getAsker().getFullName().toLowerCase().contains(k)))
                .toList();
        int from = Math.min(page * size, all.size()), to = Math.min(from + size, all.size());
        return new PageImpl<>(all.subList(from, to), PageRequest.of(page, size), all.size());
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
}

