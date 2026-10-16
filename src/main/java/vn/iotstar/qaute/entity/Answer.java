package vn.iotstar.qaute.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;
import java.util.*;

// [SHARED] Người 2 sở hữu. Người 3 chỉ dùng needApproval/approvalStatus/published
@Entity @Getter @Setter @NoArgsConstructor
public class Answer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Question question;
    @ManyToOne(optional = false) private User advisor;
    @Nationalized @Column(columnDefinition = "NVARCHAR(MAX)") private String content;
    private boolean needApproval = false;
    @Enumerated(EnumType.STRING) private ApprovalStatus approvalStatus = ApprovalStatus.NONE;
    private boolean published = true;   // false khi đang chờ duyệt / bị từ chối
    private LocalDateTime createdAt = LocalDateTime.now();
}

