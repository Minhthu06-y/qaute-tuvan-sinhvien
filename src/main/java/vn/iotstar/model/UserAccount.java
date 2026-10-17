package vn.iotstar.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_accounts", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String displayName;
    private String email;
    private String passwordHash;
    private String roles;
    private String title;
    private String studentCode;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_faculty_topics",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id"))
    private Set<FacultyTopic> assignedTopics = new HashSet<>();

    protected UserAccount() {}
    public UserAccount(String displayName, String email, String passwordHash, String roles, String title, String studentCode, Faculty faculty) {
        this.displayName = displayName; this.email = email; this.passwordHash = passwordHash;
        this.roles = roles; this.title = title; this.studentCode = studentCode; this.faculty = faculty;
    }
    public boolean hasRole(AccountRole role) { return roles != null && ("," + roles + ",").contains("," + role.name() + ","); }
    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRoles() { return roles; }
    public String getTitle() { return title; }
    public String getStudentCode() { return studentCode; }
    public Faculty getFaculty() { return faculty; }
    public Set<FacultyTopic> getAssignedTopics() { return assignedTopics; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRoles(String roles) { this.roles = roles; }
    public void setTitle(String title) { this.title = title; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public void setAssignedTopics(Collection<FacultyTopic> assignedTopics) {
        this.assignedTopics = assignedTopics == null ? new HashSet<>() : new HashSet<>(assignedTopics);
    }
    public boolean isAssignedTo(FacultyTopic topic) {
        return topic != null && assignedTopics.stream().anyMatch(assigned -> assigned.getId() != null
                && assigned.getId().equals(topic.getId()));
    }
}
