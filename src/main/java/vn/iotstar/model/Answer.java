package vn.iotstar.model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "answers")
public class Answer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "question_id")
    private Question question;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "author_id")
    private UserAccount author;
    @Column(nullable = false, length = 8000)
    private String body;
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;
    @Column(length = 1200)
    private String reviewReason;

    protected Answer() {}
    public Answer(Question question, UserAccount author, String body, LocalDateTime createdAt, ApprovalStatus approvalStatus) {
        this.question = question; this.author = author; this.body = body; this.createdAt = createdAt; this.approvalStatus = approvalStatus;
    }
    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public UserAccount getAuthor() { return author; }
    public String getBody() { return body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public String getReviewReason() { return reviewReason; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
}
