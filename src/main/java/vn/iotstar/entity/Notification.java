package vn.iotstar.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String title;

    @Nationalized
    @Column(length = 2000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Nationalized
    @Column(columnDefinition = "nvarchar(max)")
    private String message;

    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Registration announcements used the recipient name for the same relationship.
    public User getRecipient() {
        return recipient != null ? recipient : receiver;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
        this.receiver = recipient;
    }

    public User getReceiver() {
        return receiver != null ? receiver : recipient;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
        this.recipient = receiver;
    }
}

