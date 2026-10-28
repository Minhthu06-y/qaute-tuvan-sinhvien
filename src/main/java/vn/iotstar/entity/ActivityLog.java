package vn.iotstar.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Getter @Setter @NoArgsConstructor
public class ActivityLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String actorEmail;
    private String actorRole;
    private String action;
    @Nationalized private String target;
    private String ip;
    private LocalDateTime createdAt = LocalDateTime.now();
}

