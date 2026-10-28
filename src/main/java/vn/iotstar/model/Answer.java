package vn.iotstar.model;

import vn.iotstar.entity.User;
import vn.iotstar.enums.ApprovalStatus;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

/** The single persisted answer model used by the application. */
@Entity
@Table(name = "answers")
public class Answer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String body;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime editedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ApprovalStatus approvalStatus = ApprovalStatus.NOT_REQUIRED;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String reviewReason;

    protected Answer() {}

    public Answer(Question question, User author, String body, LocalDateTime createdAt, ApprovalStatus approvalStatus) {
        this.question = question;
        this.author = author;
        this.body = body;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.approvalStatus = approvalStatus == null ? ApprovalStatus.NOT_REQUIRED : approvalStatus;
    }

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public User getAdvisor() { return author; }
    public void setAdvisor(User advisor) { this.author = advisor; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getContent() { return body; }
    public void setContent(String content) { this.body = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    public String getReviewReason() { return reviewReason; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }

    public boolean isNeedApproval() { return approvalStatus == ApprovalStatus.PENDING; }
    public void setNeedApproval(boolean needApproval) {
        if (needApproval) this.approvalStatus = ApprovalStatus.PENDING;
        else if (this.approvalStatus == ApprovalStatus.PENDING) this.approvalStatus = ApprovalStatus.NOT_REQUIRED;
    }
    public boolean isPublished() {
        return approvalStatus == ApprovalStatus.APPROVED || approvalStatus == ApprovalStatus.NOT_REQUIRED;
    }
    public void setPublished(boolean published) {
        this.approvalStatus = published ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING;
    }
    public boolean isApproved() { return isPublished(); }
    public void setApproved(boolean approved) { setPublished(approved); }
}
