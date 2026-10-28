package vn.iotstar.entity;

import java.time.LocalDateTime;
import vn.iotstar.enums.*;


import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "QauteQuestion")
@Table(name = "qaute_questions")
@Getter
@Setter
@NoArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(nullable = false, length = 200)
    private String title;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String content;

    @Column(length = 100)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asker_id")
    private User asker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    private AudienceType audience;

    @Column(nullable = false)
    private boolean publicVisible;

    @Column(nullable = false)
    private boolean publicQuestion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus status = QuestionStatus.UNANSWERED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Registration backend calls the same question owner an author.
    public User getAuthor() {
        return author != null ? author : asker;
    }

    public void setAuthor(User author) {
        this.author = author;
        this.asker = author;
    }

    public User getAsker() {
        return asker != null ? asker : author;
    }

    public void setAsker(User asker) {
        this.asker = asker;
        this.author = asker;
    }

    // Keep the registration branch API while storing one public visibility flag.
    public boolean isPublicQuestion() {
        return publicVisible || publicQuestion;
    }

    public void setPublicQuestion(boolean publicQuestion) {
        this.publicQuestion = publicQuestion;
        this.publicVisible = publicQuestion;
    }

    public boolean isPublicVisible() {
        return publicVisible || publicQuestion;
    }

    public void setPublicVisible(boolean publicVisible) {
        this.publicVisible = publicVisible;
        this.publicQuestion = publicVisible;
    }
}

