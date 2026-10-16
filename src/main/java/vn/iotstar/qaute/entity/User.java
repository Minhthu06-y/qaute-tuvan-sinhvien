package vn.iotstar.qaute.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;
import java.util.*;

// [SHARED] Người 1 sở hữu - đối chiếu lại khi merge
@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String email;
    private String passwordHash;
    @Nationalized private String fullName;
    private String phone;
    @Enumerated(EnumType.STRING) private Role role = Role.USER;
    private boolean enabled = true;
    private LocalDateTime createdAt = LocalDateTime.now();
    // Dùng cho Manager: các khoa phụ trách
    @ManyToMany @JoinTable(name = "manager_departments",
        joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "department_id"))
    private Set<Department> managedDepartments = new HashSet<>();
}

