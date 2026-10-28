package vn.iotstar.model;

import java.time.LocalDateTime;
import vn.iotstar.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "question_transfers")
public class QuestionTransfer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "question_id")
    private Question question;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "from_faculty_id")
    private Faculty fromFaculty;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "to_faculty_id")
    private Faculty toFaculty;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "transferred_by_id")
    private User transferredBy;
    private LocalDateTime createdAt;
    @Column(length = 1200)
    private String reason;

    protected QuestionTransfer() {}
    public QuestionTransfer(Question question, Faculty fromFaculty, Faculty toFaculty, User transferredBy, String reason, LocalDateTime createdAt) {
        this.question = question; this.fromFaculty = fromFaculty; this.toFaculty = toFaculty; this.transferredBy = transferredBy; this.reason = reason; this.createdAt = createdAt;
    }
}
