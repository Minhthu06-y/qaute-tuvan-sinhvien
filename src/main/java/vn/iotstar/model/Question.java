package vn.iotstar.model;

import vn.iotstar.entity.Department;
import vn.iotstar.entity.User;
import vn.iotstar.enums.AudienceType;
import vn.iotstar.enums.QuestionStatus;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

/** The single persisted question model used by the application. */
@Entity
@Table(name = "questions")
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "asker_id", nullable = false)
    private User asker;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id")
    private FacultyTopic topic;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Nationalized
    @Column(nullable = false, length = 240)
    private String subject;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String body;

    @Nationalized
    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private AudienceType audience;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QuestionStatus status = QuestionStatus.UNANSWERED;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;
    private boolean locked;
    private boolean replyNotification;
    private String attachmentUrl;

    protected Question() {}

    public Question(User asker, Faculty faculty, String subject, String body, LocalDateTime createdAt) {
        this.asker = asker;
        this.faculty = faculty;
        this.subject = subject;
        this.body = body;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.status = QuestionStatus.UNANSWERED;
    }

    public Long getId() { return id; }
    public User getAsker() { return asker; }
    public void setAsker(User asker) { this.asker = asker; }
    public User getAuthor() { return asker; }
    public void setAuthor(User author) { this.asker = author; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public FacultyTopic getTopic() { return topic; }
    public void setTopic(FacultyTopic topic) {
        this.topic = topic;
        if (topic != null) this.faculty = topic.getFaculty();
    }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getTitle() { return subject; }
    public void setTitle(String title) { this.subject = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getContent() { return body; }
    public void setContent(String content) { this.body = content; }
    public String getCategory() {
        return category != null && !category.isBlank() ? category : topic == null ? null : topic.getName();
    }
    public void setCategory(String category) { this.category = category; }
    public AudienceType getAudience() { return audience; }
    public void setAudience(AudienceType audience) { this.audience = audience; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public QuestionStatus getStatus() { return status == null ? QuestionStatus.UNANSWERED : status; }
    public void setStatus(QuestionStatus status) { this.status = status; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean value) { this.isPublic = value; }
    public boolean isPublicVisible() { return isPublic; }
    public void setPublicVisible(boolean value) { this.isPublic = value; }
    public boolean isPublicQuestion() { return isPublic; }
    public void setPublicQuestion(boolean value) { this.isPublic = value; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public boolean isReplyNotification() { return replyNotification; }
    public void setReplyNotification(boolean replyNotification) { this.replyNotification = replyNotification; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
}
