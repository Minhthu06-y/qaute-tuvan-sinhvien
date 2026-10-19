package vn.iotstar.qaute.config;

import java.util.Locale;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.iotstar.model.Faculty;
import vn.iotstar.model.UserAccount;
import vn.iotstar.qaute.entity.Role;
import vn.iotstar.qaute.entity.User;
import vn.iotstar.qaute.enums.UserStatus;
import vn.iotstar.qaute.repository.UserRepository;
import vn.iotstar.repository.FacultyRepository;
import vn.iotstar.repository.UserAccountRepository;

/** Keeps manager accounts available to the legacy question portal as well as the admin portal. */
@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class LegacyManagerAccountSync implements CommandLineRunner {
    private final UserRepository qauteUsers;
    private final UserAccountRepository legacyUsers;
    private final FacultyRepository faculties;

    @Override
    @Transactional
    public void run(String... args) {
        for (User manager : qauteUsers.findByRole(Role.MANAGER)) {
            if (manager.isDeleted()) continue;
            String email = manager.getEmail() == null ? "" : manager.getEmail().trim();
            if (email.isEmpty()) continue;
            if (manager.getPasswordHash() == null || manager.getPasswordHash().isBlank()) {
                log.warn("Skipping manager sync for {} because the account has no password hash", email);
                continue;
            }

            UserAccount legacy = legacyUsers.findByEmailIgnoreCase(email).orElse(null);
            if (legacy == null) {
                Faculty faculty = manager.getManagedDepartments().stream().findFirst()
                        .flatMap(department -> faculties.findAll().stream()
                                .filter(candidate -> candidate.getName().equalsIgnoreCase(department.getName())).findFirst())
                        .orElse(null);
                legacy = new UserAccount(manager.getFullName(), email.toLowerCase(Locale.ROOT),
                        manager.getPasswordHash(), "MANAGER", "Trưởng ban cố vấn", "", faculty);
                legacy.setEnabled(manager.isEnabled() && manager.getStatus() == UserStatus.ACTIVE);
                legacyUsers.save(legacy);
                continue;
            }

            if (!legacy.hasRole(vn.iotstar.model.AccountRole.MANAGER)) {
                String roles = legacy.getRoles() == null || legacy.getRoles().isBlank()
                        ? "MANAGER" : legacy.getRoles().trim() + ",MANAGER";
                legacy.setRoles(roles);
            }
            legacy.setEnabled(manager.isEnabled() && manager.getStatus() == UserStatus.ACTIVE);
            legacyUsers.save(legacy);
        }
    }
}
