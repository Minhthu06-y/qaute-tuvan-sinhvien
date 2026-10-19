package vn.iotstar.qaute.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.iotstar.qaute.enums.UserStatus;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Nationalized
    @Column(nullable = false, length = 100)
    private String fullName;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @JsonIgnore
    @Column(name = "password_hash")
    private String passwordHash;

    @JsonIgnore
    @Column(name = "password")
    private String password;

    @Pattern(regexp = "^$|^[0-9]{10,11}$")
    @Column(length = 11)
    private String phone;

    @Column(nullable = false)
    private boolean student;

    @Column(nullable = false)
    private boolean currentlyStudying;

    @Column(length = 30)
    private String studentId;

    private LocalDate graduationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.INACTIVE;

    @Column(nullable = false)
    private boolean enabled;

    private Boolean deleted = false;

    @Column(length = 150)
    private String deletedEmail;

    private LocalDateTime deletedAt;

    @Column(length = 150)
    private String deletedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany
    @JoinTable(
        name = "manager_departments",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    private Set<Department> managedDepartments = new HashSet<>();

    // Both backend branches use a different name for the encoded credential.
    public String getPassword() {
        return password != null ? password : passwordHash;
    }

    public boolean isEnabled() {
        return enabled && !Boolean.TRUE.equals(deleted);
    }

    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }

    public void setPassword(String password) {
        this.password = password;
        this.passwordHash = password;
    }

    public String getPasswordHash() {
        return passwordHash != null ? passwordHash : password;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.password = passwordHash;
    }
}

