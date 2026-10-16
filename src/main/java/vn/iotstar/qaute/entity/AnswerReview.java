package vn.iotstar.qaute.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Getter @Setter @NoArgsConstructor
public class AnswerReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Answer answer;
    @ManyToOne(optional = false) private User manager;
    @Enumerated(EnumType.STRING) private ReviewDecision decision;
    @Nationalized @Column(columnDefinition = "NVARCHAR(MAX)") private String reason;
    @Nationalized @Column(columnDefinition = "NVARCHAR(MAX)") private String betterAnswer;
    private Integer rating; // 1-5: mức độ hiệu quả
    private LocalDateTime createdAt = LocalDateTime.now();
}

