package vn.iotstar.qaute.service;

import vn.iotstar.qaute.dto.ManagerForm;
import vn.iotstar.qaute.entity.*;
import vn.iotstar.qaute.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import vn.iotstar.qaute.enums.UserStatus;

@Service
@RequiredArgsConstructor
public class ManagerAccountService {
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;       // bean do Người 1 (SecurityConfig)
    private final ActivityLogService logs;

    public List<User> list() { return users.findByRole(Role.MANAGER); }

    @Transactional
    public User create(ManagerForm f, String actor) {
        if (users.existsByEmailIgnoreCase(f.getEmail().trim()))
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
        logs.log(actor, "ADMIN", "CREATE_MANAGER", u.getEmail());
        return u;
    }

    @Transactional
    public boolean toggleEnabled(Long id, String actor) {
        User u = users.findById(id).filter(x -> x.getRole() == Role.MANAGER)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy manager"));
        u.setEnabled(!u.isEnabled());
        logs.log(actor, "ADMIN", u.isEnabled() ? "ENABLE_MANAGER" : "DISABLE_MANAGER", u.getEmail());
        return u.isEnabled();
    }
}

