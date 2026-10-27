package vn.iotstar.service;

import vn.iotstar.dto.ManagerForm;
import vn.iotstar.entity.*;
import vn.iotstar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import vn.iotstar.enums.*;
import vn.iotstar.model.Faculty;
import vn.iotstar.model.UserAccount;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class ManagerAccountService {
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final FacultyRepository legacyFaculties;
    private final UserAccountRepository legacyUsers;
    private final PasswordEncoder encoder;       // bean do Người 1 (SecurityConfig)
    private final ActivityLogService logs;
    private final DeletedAccountEmailService deletedAccountEmails;

    public List<User> list() {
        return users.findByRole(Role.MANAGER).stream().filter(user -> !user.isDeleted()).toList();
    }

    @Transactional
    public User create(ManagerForm f, String actor) {
        deletedAccountEmails.releaseForReuse(f.getEmail().trim());
        if (users.existsByEmailIgnoreCase(f.getEmail().trim()) || legacyUsers.existsByEmailIgnoreCase(f.getEmail().trim()))
            throw new FieldException("email", "Email đã tồn tại");
        User u = new User();
        u.setEmail(f.getEmail().trim());
        u.setFullName(f.getFullName().trim());
        u.setPasswordHash(encoder.encode(f.getPassword()));
        u.setRole(Role.MANAGER);
        u.setStatus(UserStatus.ACTIVE);
        u.setEnabled(true);
        u.setManagedDepartments(new HashSet<>(departments.findAllById(f.getDepartmentIds())));
        users.save(u);
        Faculty faculty = u.getManagedDepartments().stream().findFirst()
                .flatMap(department -> legacyFaculties.findAll().stream()
                        .filter(candidate -> candidate.getName().equalsIgnoreCase(department.getName())).findFirst())
                .orElse(null);
        UserAccount legacy = new UserAccount(u.getFullName(), u.getEmail(), u.getPasswordHash(),
                "MANAGER", "Trưởng ban cố vấn", "", faculty);
        legacy.setEnabled(true);
        legacyUsers.save(legacy);
        logs.log(actor, "ADMIN", "CREATE_MANAGER", u.getEmail());
        return u;
    }

    @Transactional
    public boolean toggleEnabled(Long id, String actor) {
        User u = users.findById(id).filter(x -> x.getRole() == Role.MANAGER && !x.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy manager"));
        u.setEnabled(!u.isEnabled());
        u.setStatus(u.isEnabled() ? UserStatus.ACTIVE : UserStatus.SUSPENDED);
        legacyUsers.findByEmailIgnoreCase(u.getEmail()).ifPresent(legacy -> {
            legacy.setEnabled(u.isEnabled());
            legacyUsers.save(legacy);
        });
        logs.log(actor, "ADMIN", u.isEnabled() ? "ENABLE_MANAGER" : "DISABLE_MANAGER", u.getEmail());
        return u.isEnabled();
    }
}

