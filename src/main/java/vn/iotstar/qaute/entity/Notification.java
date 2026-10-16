package vn.iotstar.qaute.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;
import java.util.*;

// Lưu DB để người dùng offline vẫn thấy khi đăng nhập lại + đẩy realtime qua WebSocket
@Entity @Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private User receiver;
    @Nationalized @Column(columnDefinition = "NVARCHAR(MAX)") private String message;
    private String link;
    @Column(name = "is_read") private boolean read = false;
    private LocalDateTime createdAt = LocalDateTime.now();
}

