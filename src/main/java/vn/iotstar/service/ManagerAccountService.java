package vn.iotstar.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.iotstar.dto.ManagerForm;
import vn.iotstar.entity.User;
import vn.iotstar.enums.Role;
import vn.iotstar.enums.UserStatus;
import vn.iotstar.repository.DepartmentRepository;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ManagerAccountService {
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final FacultyRepository faculties;
    private final PasswordEncoder encoder;
    private final ActivityLogService logs;
    private final DeletedAccountEmailService deletedAccountEmails;

    public List<User> list() {
        return users.findByRole(Role.MANAGER).stream().filter(user -> !user.isDeleted()).toList();
    }

    public java.util.Map<Long, Set<Long>> departmentAssignments() {
        return list().stream().collect(Collectors.toMap(User::getId,
                user -> user.getManagedDepartments().stream().map(department -> department.getId())
                        .collect(Collectors.toSet())));
    }

    @Transactional
    public void updateDepartments(Long managerId, List<Long> departmentIds, String actor) {
        User manager = users.findById(managerId)
                .filter(user -> user.getRole() == Role.MANAGER && !user.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy manager"));
        Set<Long> selectedIds = departmentIds == null ? Set.of() : Set.copyOf(departmentIds);
        var selected = departments.findAllById(selectedIds);
        if (selected.size() != selectedIds.size())
            throw new IllegalArgumentException("Một hoặc nhiều khoa không tồn tại");
        manager.setManagedDepartments(new HashSet<>(selected));
        manager.setFaculty(faculties.findAll().stream()
                .filter(faculty -> selected.stream().anyMatch(department ->
                        faculty.getName().trim().equalsIgnoreCase(department.getName().trim())))
                .findFirst().orElse(null));
        users.save(manager);
        logs.log(actor, "ADMIN", "ASSIGN_MANAGER_DEPARTMENTS", manager.getEmail());
    }

    @Transactional
    public User create(ManagerForm form, String actor) {
        String email = form.getEmail().trim();
        deletedAccountEmails.releaseForReuse(email);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new FieldException("email", "Email đã tồn tại");
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(form.getFullName().trim());
        user.setPasswordHash(encoder.encode(form.getPassword()));
        user.setRole(Role.MANAGER);
        user.setConsultantTitlesCsv("MANAGER,ADVISOR");
        user.setRole(Role.MANAGER);
        user.setTitle("Trưởng ban cố vấn");
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        user.setManagedDepartments(new HashSet<>(departments.findAllById(form.getDepartmentIds())));
        user.getManagedDepartments().stream().findFirst().ifPresent(department -> faculties.findAll().stream()
                .filter(faculty -> faculty.getName().equalsIgnoreCase(department.getName()))
                .findFirst().ifPresent(user::setFaculty));
        users.save(user);
        logs.log(actor, "ADMIN", "CREATE_MANAGER", user.getEmail());
        return user;
    }

    @Transactional
    public boolean toggleEnabled(Long id, String actor) {
        User user = users.findById(id).filter(candidate -> candidate.getRole() == Role.MANAGER && !candidate.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy manager"));
        user.setEnabled(!user.isEnabled());
        user.setStatus(user.isEnabled() ? UserStatus.ACTIVE : UserStatus.SUSPENDED);
        logs.log(actor, "ADMIN", user.isEnabled() ? "ENABLE_MANAGER" : "DISABLE_MANAGER", user.getEmail());
        return user.isEnabled();
    }
}
