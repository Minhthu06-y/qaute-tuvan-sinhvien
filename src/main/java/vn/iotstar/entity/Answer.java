package vn.iotstar.entity;

import java.time.LocalDateTime;
import vn.iotstar.enums.*;


import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "QauteAnswer")
@Table(name = "qaute_answers")
@Getter
@Setter
@NoArgsConstructor
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advisor_id")
    private User advisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    private boolean needApproval;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus = ApprovalStatus.NOT_REQUIRED;

    @Column(nullable = false)
    private boolean published = true;

    @Column(nullable = false)
    private boolean approved;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Registration branch calls the answer owner an author and checks approved.
    public User getAuthor() {
        return author != null ? author : advisor;
    }

    public void setAuthor(User author) {
        this.author = author;
        this.advisor = author;
    }

    public boolean isApproved() {
        return approved || published;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
        this.published = approved;
    }

    public User getAdvisor() {
        return advisor != null ? advisor : author;
    }

    public void setAdvisor(User advisor) {
        this.advisor = advisor;
        this.author = advisor;
    }

    public boolean isPublished() {
        return published || approved;
    }

    public void setPublished(boolean published) {
        this.published = published;
        this.approved = published;
    }
}

