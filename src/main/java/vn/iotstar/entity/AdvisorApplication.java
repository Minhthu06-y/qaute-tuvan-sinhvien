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
import vn.iotstar.enums.AdvisorApplicationStatus;
import vn.iotstar.enums.AdvisorType;
import vn.iotstar.enums.AudienceType;
import vn.iotstar.entity.Department;
import vn.iotstar.model.FacultyTopic;

@Entity
@Table(name = "advisor_applications")
public class AdvisorApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private User applicant;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    private FacultyTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdvisorType advisorType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AudienceType audience;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String motivation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdvisorApplicationStatus status = AdvisorApplicationStatus.PENDING;

    @ManyToOne(fetch = FetchType.EAGER)
    private User reviewer;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String reviewNote;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public User getApplicant() { return applicant; }
    public void setApplicant(User applicant) { this.applicant = applicant; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public FacultyTopic getTopic() { return topic; }
    public void setTopic(FacultyTopic topic) { this.topic = topic; }
    public AdvisorType getAdvisorType() { return advisorType; }
    public void setAdvisorType(AdvisorType advisorType) { this.advisorType = advisorType; }
    public AudienceType getAudience() { return audience; }
    public void setAudience(AudienceType audience) { this.audience = audience; }
    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }
    public AdvisorApplicationStatus getStatus() { return status; }
    public void setStatus(AdvisorApplicationStatus status) { this.status = status; }
    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
