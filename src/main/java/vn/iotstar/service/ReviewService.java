package vn.iotstar.service;

import vn.iotstar.dto.ReviewForm;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import vn.iotstar.enums.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final QauteAnswerRepository answers;
    private final AnswerReviewRepository reviews;
    private final UserRepository users;
    private final NotificationService notifier;
    private final ActivityLogService logs;

    /** Trưởng ban cố vấn duyệt câu trả lời trên toàn hệ thống. */
    public List<Answer> listByStatus(ApprovalStatus status, String managerEmail) {
        return answers.findByStatus(status);
    }

    /** Lấy câu trả lời bất kỳ để trưởng ban cố vấn duyệt trên toàn hệ thống. */
    public Answer getForManager(Long id, String managerEmail) {
        return answers.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu trả lời"));
    }

    public List<Answer> thread(Answer a) { return answers.findByQuestion_IdOrderByCreatedAtAsc(a.getQuestion().getId()); }

    public List<AnswerReview> history(Long answerId) { return reviews.findByAnswerIdOrderByCreatedAtDesc(answerId); }

    @Transactional
    public void decide(Long answerId, ReviewForm f, String managerEmail) {
        Answer a = getForManager(answerId, managerEmail);
        if (a.getApprovalStatus() != ApprovalStatus.PENDING)
            throw new FieldException("decision", "Câu trả lời này không ở trạng thái chờ duyệt");
        if (f.getDecision() == ReviewDecision.RATED)
            throw new FieldException("decision", "Chọn duyệt hoặc từ chối");
        boolean approve = f.getDecision() == ReviewDecision.APPROVED;
        if (!approve && (f.getBetterAnswer() == null || f.getBetterAnswer().isBlank()))
            throw new FieldException("betterAnswer", "Khi từ chối, vui lòng đưa ra câu trả lời tốt hơn");
        User manager = users.findByEmailIgnoreCase(managerEmail).orElseThrow();

        a.setApprovalStatus(approve ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        a.setPublished(approve);

        AnswerReview r = new AnswerReview();
        r.setAnswer(a); r.setManager(manager); r.setDecision(f.getDecision());
        r.setReason(f.getReason().trim());
        r.setBetterAnswer(approve ? null : f.getBetterAnswer().trim());
        reviews.save(r);

        String title = a.getQuestion().getTitle();
        String msg = approve
                ? "Câu trả lời của bạn cho \"" + title + "\" đã được DUYỆT. Lý do: " + r.getReason()
                : "Câu trả lời của bạn cho \"" + title + "\" bị TỪ CHỐI. Lý do: " + r.getReason()
                  + " — Câu trả lời đề xuất: " + r.getBetterAnswer();
        notifier.notify(a.getAdvisor(), msg, "/advisor/questions/" + a.getQuestion().getId());
        logs.log(managerEmail, "MANAGER", approve ? "APPROVE_ANSWER" : "REJECT_ANSWER", "answer#" + a.getId());
    }

    /** Điểm 1-5 + nhận xét. Mỗi Manager chỉ có 1 đánh giá / câu trả lời: chấm lại = cập nhật (không nhân đôi điểm). */
    @Transactional
    public AnswerReview rate(Long answerId, int rating, String comment, String managerEmail) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Điểm từ 1 đến 5");
        Answer a = getForManager(answerId, managerEmail);
        User manager = users.findByEmailIgnoreCase(managerEmail).orElseThrow();
        AnswerReview r = reviews.findFirstByAnswerIdAndManagerIdAndDecision(a.getId(), manager.getId(), ReviewDecision.RATED)
                .orElseGet(AnswerReview::new);
        r.setAnswer(a); r.setManager(manager); r.setDecision(ReviewDecision.RATED);
        r.setRating(rating);
        r.setReason(comment == null || comment.isBlank() ? null : comment.trim());
        r.setCreatedAt(LocalDateTime.now());
        reviews.save(r);
        logs.log(managerEmail, "MANAGER", "RATE_ANSWER", "answer#" + a.getId() + " = " + rating);
        return r;
    }
}

