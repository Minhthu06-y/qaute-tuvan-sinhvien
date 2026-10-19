package vn.iotstar.qaute.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.iotstar.model.AccountRole;
import vn.iotstar.model.UserAccount;
import vn.iotstar.qaute.entity.AdvisorProfile;
import vn.iotstar.qaute.entity.AdvisorStatus;
import vn.iotstar.qaute.entity.AdvisorType;
import vn.iotstar.qaute.entity.AudienceType;
import vn.iotstar.qaute.entity.Department;
import vn.iotstar.qaute.entity.Role;
import vn.iotstar.qaute.entity.User;
import vn.iotstar.qaute.enums.UserStatus;
import vn.iotstar.qaute.repository.AdvisorProfileRepository;
import vn.iotstar.qaute.repository.DepartmentRepository;
import vn.iotstar.qaute.repository.UserRepository;
import vn.iotstar.repository.UserAccountRepository;

/** Imports existing advisor accounts from the original account model into admin advisor profiles. */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class LegacyAdvisorProfileSync implements CommandLineRunner {
    private final UserAccountRepository legacyUsers;
    private final UserRepository users;
    private final AdvisorProfileRepository advisors;
    private final DepartmentRepository departments;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Department> departmentIndex = new HashMap<>();
        departments.findAll().forEach(department ->
                departmentIndex.put(normalize(department.getName()), department));

        int imported = 0;
        for (UserAccount legacy : legacyUsers.findAll()) {
            if (legacy.isDeleted() || !isAdvisorAccount(legacy)) continue;

            String email = legacy.getEmail() == null ? "" : legacy.getEmail().trim();
            if (email.isEmpty()) continue;

            User user = users.findByEmailIgnoreCase(email).orElse(null);
            if (user == null) {
                if (legacy.getPasswordHash() == null || legacy.getPasswordHash().isBlank()) {
                    log.warn("Skipping legacy advisor {} because the account has no password hash", email);
                    continue;
                }
                user = new User();
                user.setEmail(email.toLowerCase(Locale.ROOT));
                user.setPasswordHash(legacy.getPasswordHash());
                user.setRole(Role.USER);
                user.setStatus(UserStatus.ACTIVE);
                user.setEnabled(true);
                user.setStudent(legacy.hasRole(AccountRole.STUDENT));
                user.setCurrentlyStudying(legacy.hasRole(AccountRole.STUDENT)
                        && legacy.getStudentCode() != null && !legacy.getStudentCode().isBlank());
                if (legacy.hasRole(AccountRole.STUDENT)) user.setStudentId(legacy.getStudentCode());
            }
            if (user.getFullName() == null || user.getFullName().isBlank()) {
                user.setFullName(legacy.getDisplayName() == null || legacy.getDisplayName().isBlank()
                        ? email : legacy.getDisplayName().trim());
            }
            user = users.save(user);

            // Keep removed profiles removed and avoid creating duplicate profiles on later starts.
            AdvisorProfile existingProfile = advisors.findByUserId(user.getId()).orElse(null);
            if (existingProfile != null) {
                if (!existingProfile.isDeleted() && existingProfile.getAdvisorType() == null) {
                    existingProfile.setAdvisorType(resolveAdvisorType(legacy));
                    advisors.save(existingProfile);
                }
                continue;
            }

            Department department = resolveDepartment(legacy, departmentIndex);
            AdvisorProfile profile = new AdvisorProfile();
            profile.setUser(user);
            profile.setDepartment(department);
            profile.setAdvisorType(resolveAdvisorType(legacy));
            profile.setAudience(AudienceType.SINH_VIEN);
            profile.setStatus(AdvisorStatus.ACTIVE);
            advisors.save(profile);
            imported++;
        }

        for (AdvisorProfile profile : advisors.findAll()) {
            if (profile.isDeleted()) continue;
            if (profile.getAdvisorType() == null) profile.setAdvisorType(AdvisorType.STUDENT);
            if (profile.getAudience() == null) profile.setAudience(AudienceType.SINH_VIEN);
        }

        if (imported > 0) log.info("Imported {} existing advisor account(s) into admin advisor profiles", imported);
    }

    private Department resolveDepartment(UserAccount legacy, Map<String, Department> index) {
        String name = legacy.getFaculty() == null ? null : legacy.getFaculty().getName();
        String key = normalize(name);
        Department department = index.get(key);
        if (department != null) return department;

        if (name != null && !name.isBlank()) {
            department = departments.save(new Department(name.trim()));
            index.put(key, department);
            return department;
        }

        return index.values().stream().findFirst()
                .orElseGet(() -> departments.save(new Department("Chưa phân khoa")));
    }

    private boolean isAdvisorAccount(UserAccount legacy) {
        return legacy.hasRole(AccountRole.ADVISOR)
                || legacy.hasRole(AccountRole.LECTURER)
                || legacy.hasRole(AccountRole.STAFF)
                || legacy.hasRole(AccountRole.MANAGER);
    }

    private AdvisorType resolveAdvisorType(UserAccount legacy) {
        if (legacy.hasRole(AccountRole.MANAGER)) return AdvisorType.TEAM_LEADER;
        if (legacy.hasRole(AccountRole.STAFF)) return AdvisorType.STAFF;
        if (legacy.hasRole(AccountRole.LECTURER)) return AdvisorType.LECTURER;
        return AdvisorType.STUDENT;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
