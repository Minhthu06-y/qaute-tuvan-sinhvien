package vn.iotstar.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.iotstar.enums.QuestionReportStatus;
import vn.iotstar.model.Question;

@Entity
@Table(name = "question_reports")
public class QuestionReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Question question;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private User reporter;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionReportStatus status = QuestionReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.EAGER)
    private User reviewer;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String reviewNote;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public QuestionReportStatus getStatus() { return status; }
    public void setStatus(QuestionReportStatus status) { this.status = status; }
    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
