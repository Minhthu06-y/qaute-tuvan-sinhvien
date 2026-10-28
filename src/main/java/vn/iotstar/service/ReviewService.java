package vn.iotstar.service;

import vn.iotstar.dto.ReviewForm;
import vn.iotstar.entity.AnswerReview;
import vn.iotstar.entity.User;
import vn.iotstar.model.Answer;
import vn.iotstar.repository.AnswerRepository;
import vn.iotstar.repository.AnswerReviewRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.enums.ApprovalStatus;
import vn.iotstar.enums.ReviewDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final AnswerRepository answers;
    private final AnswerReviewRepository reviews;
    private final UserRepository users;
    private final NotificationService notifier;
    private final ActivityLogService logs;
    private final ScopeService scope;

    public List<Answer> listByStatus(ApprovalStatus status, String managerEmail) {
        Set<Long> allowed = scope.managerDepartments(managerEmail);
        return answers.findByApprovalStatusOrderByCreatedAtAsc(status).stream()
                .filter(answer -> ScopeService.allows(allowed, answer.getQuestion().getDepartment()))
                .toList();
    }

    public Answer getForManager(Long id, String managerEmail) {
        Answer answer = answers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found"));
        if (!ScopeService.allows(scope.managerDepartments(managerEmail), answer.getQuestion().getDepartment()))
            throw new AccessDeniedException("Answer is outside the manager's departments");
        return answer;
    }

    public List<Answer> thread(Answer answer) {
        return answers.findByQuestionIdOrderByCreatedAtAsc(answer.getQuestion().getId());
    }

    public List<AnswerReview> history(Long answerId) {
        return reviews.findByAnswerIdOrderByCreatedAtDesc(answerId);
    }

    @Transactional
    public void decide(Long answerId, ReviewForm form, String managerEmail) {
        Answer answer = getForManager(answerId, managerEmail);
        if (answer.getApprovalStatus() != ApprovalStatus.PENDING)
            throw new FieldException("decision", "Câu trả lời này không ở trạng thái chờ duyệt");
        if (form.getDecision() == ReviewDecision.RATED)
            throw new FieldException("decision", "Chọn duyệt hoặc từ chối");
        boolean approve = form.getDecision() == ReviewDecision.APPROVED;
        if (!approve && (form.getBetterAnswer() == null || form.getBetterAnswer().isBlank()))
            throw new FieldException("betterAnswer", "Khi từ chối, vui lòng đưa ra câu trả lời tốt hơn");
        User manager = users.findByEmailIgnoreCase(managerEmail).orElseThrow();

        answer.setApprovalStatus(approve ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        vn.iotstar.model.Question question = answer.getQuestion();
        if (approve) {
            question.setStatus(vn.iotstar.enums.QuestionStatus.ANSWERED);
            question.setReplyNotification(true);
        } else if (!question.isLocked()) {
            boolean hasPublishedAnswer = answers.existsByQuestionIdAndApprovalStatus(
                    question.getId(), ApprovalStatus.APPROVED)
                    || answers.existsByQuestionIdAndApprovalStatus(question.getId(), ApprovalStatus.NOT_REQUIRED);
            question.setStatus(hasPublishedAnswer ? vn.iotstar.enums.QuestionStatus.ANSWERED
                    : vn.iotstar.enums.QuestionStatus.UNANSWERED);
        }
        AnswerReview review = new AnswerReview();
        review.setAnswer(answer);
        review.setManager(manager);
        review.setDecision(form.getDecision());
        review.setReason(form.getReason().trim());
        review.setBetterAnswer(approve ? null : form.getBetterAnswer().trim());
        reviews.save(review);

        String title = question.getTitle();
        String message = approve
                ? "Câu trả lời của bạn cho \"" + title + "\" đã được duyệt. Lý do: " + review.getReason()
                : "Câu trả lời của bạn cho \"" + title + "\" bị từ chối. Lý do: " + review.getReason()
                        + " — Câu trả lời đề xuất: " + review.getBetterAnswer();
        notifier.notify(answer.getAuthor(), message, "/questions/" + answer.getQuestion().getId());
        if (question.getAsker() != null)
            notifier.notify(question.getAsker(), message, "/questions/" + question.getId());
        logs.log(managerEmail, "MANAGER", approve ? "APPROVE_ANSWER" : "REJECT_ANSWER", "answer#" + answer.getId());
    }

    @Transactional
    public AnswerReview rate(Long answerId, int rating, String comment, String managerEmail) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Điểm phải từ 1 đến 5");
        Answer answer = getForManager(answerId, managerEmail);
        User manager = users.findByEmailIgnoreCase(managerEmail).orElseThrow();
        AnswerReview review = reviews.findFirstByAnswerIdAndManagerIdAndDecision(
                        answer.getId(), manager.getId(), ReviewDecision.RATED)
                .orElseGet(AnswerReview::new);
        review.setAnswer(answer);
        review.setManager(manager);
        review.setDecision(ReviewDecision.RATED);
        review.setRating(rating);
        review.setReason(comment == null || comment.isBlank() ? null : comment.trim());
        review.setCreatedAt(LocalDateTime.now());
        reviews.save(review);
        logs.log(managerEmail, "MANAGER", "RATE_ANSWER", "answer#" + answer.getId() + " = " + rating);
        return review;
    }
}
