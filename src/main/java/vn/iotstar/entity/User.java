package vn.iotstar.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.Nationalized;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.iotstar.enums.AdvisorType;
import vn.iotstar.enums.Role;
import vn.iotstar.enums.UserStatus;
import vn.iotstar.model.Faculty;
import vn.iotstar.model.FacultyTopic;

/** The single account entity for web, advising, API and administration features. */
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

    /** The user's single system-wide authorization role. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    /** Consultant titles are presentation data and never grant application permissions. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_consultant_titles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "title", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Set<AdvisorType> consultantTitles = new HashSet<>();

    @Column(length = 160)
    private String title;

    @Column(name = "student_code", length = 30)
    private String studentCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_assigned_topics",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id"))
    private Set<FacultyTopic> assignedTopics = new HashSet<>();

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

    /** Imports the former account model's comma-separated consultant titles. */
    public User(String displayName, String email, String passwordHash, String oldTitles,
            String title, String studentCode, Faculty faculty) {
        this.fullName = displayName;
        this.email = email;
        setPasswordHash(passwordHash);
        setConsultantTitlesCsv(oldTitles);
        this.title = title;
        this.studentCode = studentCode;
        this.faculty = faculty;
        this.student = consultantTitles.contains(AdvisorType.STUDENT);
        this.currentlyStudying = this.student && studentCode != null && !studentCode.isBlank();
        this.studentId = this.student ? studentCode : null;
        this.enabled = true;
        this.status = UserStatus.ACTIVE;
    }

    // Compatibility accessors keep existing templates and API DTOs stable during the merge.
    public String getDisplayName() { return fullName; }
    public void setDisplayName(String displayName) { this.fullName = displayName; }
    public String getStudentCode() { return studentCode != null ? studentCode : studentId; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getPassword() { return password != null ? password : passwordHash; }
    public void setPassword(String password) {
        this.password = password;
        this.passwordHash = password;
    }
    public String getPasswordHash() { return passwordHash != null ? passwordHash : password; }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.password = passwordHash;
    }

    public boolean isEnabled() { return enabled && !isDeleted(); }
    public boolean isDeleted() { return Boolean.TRUE.equals(deleted); }

    public void setConsultantTitlesCsv(String csv) {
        consultantTitles = new HashSet<>();
        if (csv == null || csv.isBlank()) return;
        for (String value : csv.split(",")) {
            try {
                String title = value.trim().toUpperCase(Locale.ROOT);
                consultantTitles.add("MANAGER".equals(title)
                        ? AdvisorType.TEAM_LEADER : AdvisorType.valueOf(title));
            } catch (IllegalArgumentException ignored) {
                // Ignore legacy labels that do not describe consultant titles.
            }
        }
        if (consultantTitles.contains(AdvisorType.STUDENT)) student = true;
        if (consultantTitles.stream().anyMatch(title -> title != AdvisorType.STUDENT) && role == Role.USER) {
            role = Role.CONSULTANT;
        }
    }

    public boolean hasConsultantTitle(AdvisorType title) {
        return title != null && consultantTitles.contains(title);
    }

    public String getConsultantTitlesCsv() {
        return consultantTitles.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    public void setAssignedTopics(Collection<FacultyTopic> assignedTopics) {
        this.assignedTopics = assignedTopics == null ? new HashSet<>() : new HashSet<>(assignedTopics);
    }

    public boolean isAssignedTo(FacultyTopic topic) {
        return topic != null && assignedTopics.stream().anyMatch(assigned -> assigned.getId() != null
                && assigned.getId().equals(topic.getId()));
    }
}
