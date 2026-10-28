package vn.iotstar.config;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import vn.iotstar.enums.UserStatus;
import vn.iotstar.model.Faculty;
import vn.iotstar.model.FacultyTopic;
import vn.iotstar.repository.AdvisorProfileRepository;
import vn.iotstar.repository.DepartmentRepository;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.FacultyTopicRepository;
import vn.iotstar.repository.UserRepository;

/** Ensures each department has an active manager and consultant in local/demo databases. */
@Component
@Profile("local")
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class DepartmentDemoAccountSeeder implements CommandLineRunner {
    private static final Map<String, Integer> LOCAL_TEST_ACCOUNT_NAME_INDICES = Map.of(
            "manager@qaute.edu.vn", 80,
            "canbo@qaute.edu.vn", 80,
            "giangvien@qaute.edu.vn", 81,
            "tuvanvien@qaute.edu.vn", 82);
    private static final Map<String, AdvisorType> LOCAL_TEST_ACCOUNT_TITLES = Map.of(
            "canbo@qaute.edu.vn", AdvisorType.STAFF,
            "giangvien@qaute.edu.vn", AdvisorType.LECTURER,
            "tuvanvien@qaute.edu.vn", AdvisorType.ADVISOR);

    private final UserRepository users;
    private final DepartmentRepository departments;
    private final AdvisorProfileRepository advisorProfiles;
    private final FacultyRepository faculties;
    private final FacultyTopicRepository facultyTopics;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.demo-account-password:123456}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(String... args) {
        List<Department> allDepartments = departments.findAll();
        if (allDepartments.isEmpty()) {
            log.warn("No departments found; skipped local demo account bootstrap");
            return;
        }

        List<User> allUsers = users.findAll();
        refreshGeneratedDepartmentAccountNames(allUsers);
        List<AdvisorProfile> allProfiles = advisorProfiles.findAll();
        refreshGeneratedConsultantTitles(allProfiles);
        List<Faculty> allFaculties = faculties.findAll();
        int managersCreated = 0;
        int managerAssignmentsAdded = 0;
        int consultantsCreated = 0;

        for (Department department : allDepartments) {
            if (!hasActiveManager(department, allUsers)) {
                User matchingManager = findUnassignedManagerForFaculty(department, allUsers, allFaculties);
                if (matchingManager != null) {
                    matchingManager.getManagedDepartments().add(department);
                    users.save(matchingManager);
                    managerAssignmentsAdded++;
                } else {
                    User manager = newUser("manager", department, allFaculties);
                    manager.setRole(Role.MANAGER);
                    manager.setTitle("Manager");
                    manager.getManagedDepartments().add(department);
                    users.save(manager);
                    allUsers.add(manager);
                    managersCreated++;
                }
            }

            if (!hasActiveConsultant(department, allProfiles)) {
                Faculty faculty = facultyFor(department, allFaculties);
                User consultant = newUser("consultant", department, allFaculties);
                consultant.setRole(Role.CONSULTANT);
                AdvisorType advisorType = DemoAdvisorTitles.forIndex(department.getId().intValue());
                consultant.setTitle(advisorType.getLabel());
                consultant.setConsultantTitles(Set.of(advisorType));
                if (faculty != null) {
                    consultant.setFaculty(faculty);
                    consultant.setAssignedTopics(new HashSet<>(facultyTopics.findByFacultyIdOrderByIdAsc(faculty.getId())));
                }
                users.save(consultant);

                AdvisorProfile profile = new AdvisorProfile();
                profile.setUser(consultant);
                profile.setDepartment(department);
                profile.setAdvisorType(advisorType);
                profile.setAudience(AudienceType.SINH_VIEN);
                profile.setStatus(AdvisorStatus.ACTIVE);
                profile.setDeleted(false);
                advisorProfiles.save(profile);
                allProfiles.add(profile);
                allUsers.add(consultant);
                consultantsCreated++;
            }
        }

        if (managersCreated > 0 || managerAssignmentsAdded > 0 || consultantsCreated > 0) {
            log.info("Local account bootstrap: created {} manager(s), added {} manager department assignment(s), "
                    + "created {} consultant(s) across {} department(s)", managersCreated,
                    managerAssignmentsAdded, consultantsCreated, allDepartments.size());
        } else {
            log.info("Local account bootstrap: all {} department(s) already have an active manager and consultant",
                    allDepartments.size());
        }
    }

    private boolean hasActiveManager(Department department, List<User> allUsers) {
        return allUsers.stream().anyMatch(user -> isActive(user, Role.MANAGER)
                && user.getManagedDepartments().stream().anyMatch(assigned -> department.getId().equals(assigned.getId())));
    }

    private boolean hasActiveConsultant(Department department, List<AdvisorProfile> allProfiles) {
        return allProfiles.stream().anyMatch(profile -> !profile.isDeleted()
                && profile.getStatus() == AdvisorStatus.ACTIVE
                && profile.getDepartment() != null
                && department.getId().equals(profile.getDepartment().getId())
                && isActive(profile.getUser(), Role.CONSULTANT));
    }

    private User findUnassignedManagerForFaculty(Department department, List<User> allUsers,
            List<Faculty> allFaculties) {
        Faculty matchingFaculty = facultyFor(department, allFaculties);
        if (matchingFaculty == null) return null;
        return allUsers.stream()
                .filter(user -> isActive(user, Role.MANAGER))
                .filter(user -> user.getManagedDepartments().isEmpty())
                .filter(user -> user.getFaculty() != null
                        && matchingFaculty.getId().equals(user.getFaculty().getId()))
                .findFirst().orElse(null);
    }

    private User newUser(String rolePrefix, Department department, List<Faculty> allFaculties) {
        String email = availableEmail(rolePrefix, department.getId());
        User user = new User();
        user.setEmail(email);
        Role role = "manager".equals(rolePrefix) ? Role.MANAGER : Role.CONSULTANT;
        user.setFullName(DemoPersonNames.forRole(role, department.getId().intValue()));
        if (role == Role.CONSULTANT) {
            AdvisorType advisorType = DemoAdvisorTitles.forIndex(department.getId().intValue());
            user.setTitle(advisorType.getLabel());
            user.setConsultantTitles(new HashSet<>(Set.of(advisorType)));
        }
        user.setPasswordHash(passwordEncoder.encode(demoPassword));
        user.setRole("manager".equals(rolePrefix) ? Role.MANAGER : Role.CONSULTANT);
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        user.setDeleted(false);
        user.setFaculty(facultyFor(department, allFaculties));
        return user;
    }

    private void refreshGeneratedDepartmentAccountNames(List<User> allUsers) {
        for (User user : allUsers) {
            Role role = user.getRole();
            if (role != Role.MANAGER && role != Role.CONSULTANT) continue;
            String email = user.getEmail() == null ? "" : user.getEmail().toLowerCase(Locale.ROOT);
            Integer nameIndex = LOCAL_TEST_ACCOUNT_NAME_INDICES.get(email);
            if (nameIndex == null) {
                String prefix = role == Role.MANAGER ? "manager.dept" : "consultant.dept";
                if (!email.startsWith(prefix)) continue;

                String suffix = email.substring(prefix.length());
                int end = 0;
                while (end < suffix.length() && Character.isDigit(suffix.charAt(end))) end++;
                if (end == 0) continue;
                try {
                    nameIndex = Integer.parseInt(suffix.substring(0, end));
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
            user.setFullName(DemoPersonNames.forRole(role, nameIndex));
            users.save(user);
        }
    }

    private void refreshGeneratedConsultantTitles(List<AdvisorProfile> allProfiles) {
        for (AdvisorProfile profile : allProfiles) {
            User user = profile.getUser();
            if (profile.isDeleted() || user == null || user.getRole() != Role.CONSULTANT) continue;
            String email = user.getEmail() == null ? "" : user.getEmail().toLowerCase(Locale.ROOT);
            AdvisorType advisorType = LOCAL_TEST_ACCOUNT_TITLES.get(email);
            Integer index = generatedConsultantIndex(email);
            if (advisorType == null && index != null) advisorType = DemoAdvisorTitles.forIndex(index);
            if (advisorType == null) continue;

            profile.setAdvisorType(advisorType);
            user.setTitle(advisorType.getLabel());
            user.setConsultantTitles(new HashSet<>(Set.of(advisorType)));
            users.save(user);
            advisorProfiles.save(profile);
        }
    }

    private Integer generatedConsultantIndex(String email) {
        String prefix;
        if (email.startsWith("demo.consultant")) prefix = "demo.consultant";
        else if (email.startsWith("consultant.dept")) prefix = "consultant.dept";
        else return null;

        String suffix = email.substring(prefix.length());
        int end = 0;
        while (end < suffix.length() && Character.isDigit(suffix.charAt(end))) end++;
        if (end == 0) return null;
        try {
            return Integer.parseInt(suffix.substring(0, end));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String availableEmail(String rolePrefix, Long departmentId) {
        String localPart = rolePrefix + ".dept" + departmentId;
        String email = localPart + "@qaute.edu.vn";
        int suffix = 1;
        while (users.existsByEmailIgnoreCase(email)) {
            email = localPart + ".seed" + suffix++ + "@qaute.edu.vn";
        }
        return email;
    }

    private boolean isActive(User user, Role role) {
        return user != null && user.getRole() == role && user.getStatus() == UserStatus.ACTIVE
                && user.isEnabled() && !user.isDeleted();
    }

    private Faculty facultyFor(Department department, List<Faculty> allFaculties) {
        String departmentName = normalize(department.getName());
        return allFaculties.stream()
                .filter(faculty -> {
                    String facultyName = normalize(faculty.getName());
                    return facultyName.equals(departmentName) || facultyName.startsWith(departmentName)
                            || departmentName.startsWith(facultyName);
                })
                .findFirst().orElse(null);
    }

    private String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", "");
    }
}
