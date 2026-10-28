package vn.iotstar.model;

import vn.iotstar.enums.*;
import java.time.LocalDateTime;
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
import jakarta.persistence.Column;

@Entity
@Table(name = "questions")
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "asker_id")
    private UserAccount asker;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "faculty_id")
    private Faculty faculty;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "topic_id")
    private FacultyTopic topic;
    @Column(nullable = false, length = 240)
    private String subject;
    @Column(nullable = false, length = 8000)
    private String body;
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    private QuestionStatus status;
    private boolean isPublic;
    private boolean locked;
    private boolean replyNotification;
    private String attachmentUrl;

    protected Question() {}
    public Question(UserAccount asker, Faculty faculty, String subject, String body, LocalDateTime createdAt) {
        this.asker = asker; this.faculty = faculty; this.subject = subject; this.body = body;
        this.createdAt = createdAt; this.status = QuestionStatus.OPEN; this.isPublic = false; this.replyNotification = false;
    }
    public Long getId() { return id; }
    public UserAccount getAsker() { return asker; }
    public Faculty getFaculty() { return faculty; }
    public FacultyTopic getTopic() { return topic; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public QuestionStatus getStatus() { return status; }
    public boolean isPublic() { return isPublic; }
    public boolean isLocked() { return locked; }
    public boolean isReplyNotification() { return replyNotification; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public void setTopic(FacultyTopic topic) {
        this.topic = topic;
        if (topic != null) this.faculty = topic.getFaculty();
    }
    public void setStatus(QuestionStatus status) { this.status = status; }
    public void setPublic(boolean value) { this.isPublic = value; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public void setReplyNotification(boolean replyNotification) { this.replyNotification = replyNotification; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
}
