package vn.iotstar.qaute.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;
import java.util.*;

// [SHARED] Người 1/2 sở hữu
@Entity @Getter @Setter @NoArgsConstructor
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Nationalized private String title;
    @Nationalized @Column(columnDefinition = "NVARCHAR(MAX)") private String content;
    @ManyToOne private User asker;
    @ManyToOne private Department department;
    @Enumerated(EnumType.STRING) private AudienceType audience;
    @Enumerated(EnumType.STRING) private QuestionStatus status = QuestionStatus.UNANSWERED;
    private boolean publicVisible = false;
    private LocalDateTime createdAt = LocalDateTime.now();
}

