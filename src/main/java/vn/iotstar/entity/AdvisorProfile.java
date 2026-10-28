package vn.iotstar.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;
import java.util.*;
import vn.iotstar.enums.*;

// Hồ sơ tư vấn viên và phân công chức danh theo khoa/đơn vị.
@Entity @Getter @Setter @NoArgsConstructor
public class AdvisorProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(optional = false) @JoinColumn(unique = true) private User user;
    @ManyToOne(optional = false) private Department department;
    @Enumerated(EnumType.STRING) private AdvisorType advisorType = AdvisorType.ADVISOR;
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

