package vn.iotstar.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.iotstar.entity.AdvisorProfile;
import vn.iotstar.entity.Department;
import vn.iotstar.entity.User;
import vn.iotstar.enums.AdvisorStatus;
import vn.iotstar.enums.AdvisorType;
import vn.iotstar.enums.AudienceType;
import vn.iotstar.enums.Role;
import vn.iotstar.repository.AdvisorProfileRepository;
import vn.iotstar.repository.DepartmentRepository;
import vn.iotstar.repository.UserRepository;

/** Ensures consultant accounts have an admin advisor profile. */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class AdvisorProfileBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final AdvisorProfileRepository advisors;
    private final DepartmentRepository departments;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Department> departmentIndex = new HashMap<>();
        departments.findAll().forEach(department ->
                departmentIndex.put(normalize(department.getName()), department));

        int created = 0;
        for (User user : users.findAll()) {
            if (user.isDeleted() || !isAdvisorAccount(user)) continue;
            AdvisorProfile profile = advisors.findByUserId(user.getId()).orElse(null);
            if (profile != null) {
                if (!profile.isDeleted() && profile.getAdvisorType() == null) {
                    profile.setAdvisorType(resolveAdvisorType(user));
                    advisors.save(profile);
                }
                continue;
            }

            profile = new AdvisorProfile();
            profile.setUser(user);
            profile.setDepartment(resolveDepartment(user, departmentIndex));
            profile.setAdvisorType(resolveAdvisorType(user));
            profile.setAudience(AudienceType.SINH_VIEN);
            profile.setStatus(AdvisorStatus.ACTIVE);
            advisors.save(profile);
            created++;
        }

        for (AdvisorProfile profile : advisors.findAll()) {
            if (profile.isDeleted()) continue;
            User user = profile.getUser();
            if (profile.getDepartment() == null && user != null) {
                profile.setDepartment(resolveDepartment(user, departmentIndex));
            }
            if (profile.getAdvisorType() == null) profile.setAdvisorType(resolveAdvisorType(user));
            if (profile.getAudience() == null) profile.setAudience(AudienceType.SINH_VIEN);
            if (profile.getStatus() == null) profile.setStatus(AdvisorStatus.ACTIVE);
        }

        if (created > 0) log.info("Created {} advisor profile(s) for accounts with advising roles", created);
    }

    private Department resolveDepartment(User user, Map<String, Department> index) {
        String name = user.getFaculty() == null ? null : user.getFaculty().getName();
        Department department = index.get(normalize(name));
        if (department != null) return department;
        if (name != null && !name.isBlank()) {
            department = departments.save(new Department(name.trim()));
            index.put(normalize(name), department);
            return department;
        }
        return index.values().stream().findFirst()
                .orElseGet(() -> departments.save(new Department("Chưa phân khoa")));
    }

    private boolean isAdvisorAccount(User user) {
        return user.getRole() == Role.CONSULTANT;
    }

    private AdvisorType resolveAdvisorType(User user) {
        if (user == null) return AdvisorType.ADVISOR;
        if (user.hasConsultantTitle(AdvisorType.TEAM_LEADER)) return AdvisorType.TEAM_LEADER;
        if (user.hasConsultantTitle(AdvisorType.STAFF)) return AdvisorType.STAFF;
        if (user.hasConsultantTitle(AdvisorType.LECTURER)) return AdvisorType.LECTURER;
        if (user.hasConsultantTitle(AdvisorType.ADVISOR)) return AdvisorType.ADVISOR;
        return AdvisorType.ADVISOR;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
