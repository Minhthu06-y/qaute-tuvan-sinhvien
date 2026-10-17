package vn.iotstar.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications")
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    // Chỉ lưu mã OTP đã băm, không lưu OTP dạng rõ.
    @Column(nullable = false)
    private String otpHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime lastSentAt;

    @Column(nullable = false)
    private int attempts = 0;

    public OtpVerification() {}

    public OtpVerification(String email, String otpHash,
                           LocalDateTime expiresAt,
                           LocalDateTime lastSentAt) {
        this.email = email;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
        this.lastSentAt = lastSentAt;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getOtpHash() { return otpHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getLastSentAt() { return lastSentAt; }
    public int getAttempts() { return attempts; }

    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    public void setLastSentAt(LocalDateTime lastSentAt) {
        this.lastSentAt = lastSentAt;
    }
    public void setAttempts(int attempts) { this.attempts = attempts; }
}