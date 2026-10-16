package vn.iotstar.qaute.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;
import java.util.*;

// [CỦA NGƯỜI 3] Hồ sơ tư vấn viên. Role của User vẫn là USER; quyền tư vấn = có profile ACTIVE.
@Entity @Getter @Setter @NoArgsConstructor
public class AdvisorProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false) @JoinColumn(unique = true) private User user;
    @ManyToOne(optional = false) private Department department;
    @Enumerated(EnumType.STRING) private AudienceType audience;
    @Enumerated(EnumType.STRING) private AdvisorStatus status = AdvisorStatus.ACTIVE;
    /** Trưởng chuyên viên tư vấn của khoa (mỗi khoa tối đa 1) */
    private boolean head = false;
    /** Xoá mềm: gỡ khỏi đội ngũ nhưng GIỮ hồ sơ + toàn bộ câu hỏi/trả lời/log. */
    private boolean deleted = false;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private LocalDateTime createdAt = LocalDateTime.now();
}

