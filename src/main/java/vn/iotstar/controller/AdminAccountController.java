package vn.iotstar.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.iotstar.model.UserAccount;
import vn.iotstar.entity.AdvisorProfile;
import vn.iotstar.entity.User;
import vn.iotstar.enums.*;
import vn.iotstar.repository.AdvisorProfileRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.ActivityLogService;
import vn.iotstar.service.DeletedAccountEmailService;
import vn.iotstar.repository.UserAccountRepository;

@Controller
@RequestMapping("/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAccountController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository qauteUsers;
    private final UserAccountRepository legacyUsers;
    private final AdvisorProfileRepository advisorProfiles;
    private final ActivityLogService logs;
    private final DeletedAccountEmailService deletedAccountEmails;

    @GetMapping
    public String list(@RequestParam(required = false) String kw,
                       @RequestParam(defaultValue = "ALL") String role,
                       @RequestParam(defaultValue = "ALL") String status,
                       Model model) {
        String search = kw == null ? "" : kw.trim().toLowerCase(Locale.ROOT);
        String selectedRole = role == null ? "ALL" : role.trim().toUpperCase(Locale.ROOT);
        String selectedStatus = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);

        List<AccountRow> allAccounts = allAccounts();
        List<AccountRow> accounts = allAccounts.stream()
                .filter(account -> search.isEmpty()
                        || account.fullName().toLowerCase(Locale.ROOT).contains(search)
                        || account.email().toLowerCase(Locale.ROOT).contains(search))
                .filter(account -> selectedRole.equals("ALL") || account.roleCodes().contains(selectedRole))
                .filter(account -> selectedStatus.equals("ALL") || account.statusCode().equals(selectedStatus))
                .sorted(Comparator.comparing(AccountRow::fullName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AccountRow::email, String.CASE_INSENSITIVE_ORDER))
                .toList();

        model.addAttribute("accounts", accounts);
        model.addAttribute("kw", kw);
        model.addAttribute("role", selectedRole);
        model.addAttribute("status", selectedStatus);
        model.addAttribute("roleOptions", roleOptions());
        model.addAttribute("activeCount", allAccounts.stream().filter(a -> a.statusCode().equals("ACTIVE")).count());
        model.addAttribute("lockedCount", allAccounts.stream().filter(a -> a.statusCode().equals("DISABLED")).count());
        model.addAttribute("totalCount", allAccounts.size());
        return "admin/accounts";
    }

    @PostMapping("/toggle")
    public String toggle(@RequestParam String email, Principal principal, RedirectAttributes flash) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.equalsIgnoreCase(principal.getName())) {
            flash.addFlashAttribute("error", "Không thể khóa tài khoản quản trị viên đang đăng nhập.");
            return "redirect:/admin/accounts";
        }

        User qauteUser = qauteUsers.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        UserAccount legacyUser = legacyUsers.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (qauteUser == null && legacyUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (qauteUser != null && qauteUser.getRole() == Role.ADMIN) {
            flash.addFlashAttribute("error", "Không thể khóa tài khoản quản trị viên.");
            return "redirect:/admin/accounts";
        }

        boolean currentlyEnabled = isActive(qauteUser, legacyUser);
        boolean enabled = !currentlyEnabled;
        if (qauteUser != null) {
            qauteUser.setEnabled(enabled);
            qauteUser.setStatus(enabled ? UserStatus.ACTIVE : UserStatus.SUSPENDED);
            qauteUsers.save(qauteUser);
        }
        if (legacyUser != null) {
            legacyUser.setEnabled(enabled);
            legacyUsers.save(legacyUser);
        }

        logs.log(principal.getName(), "ADMIN", enabled ? "ENABLE_ACCOUNT" : "DISABLE_ACCOUNT", normalizedEmail);
        flash.addFlashAttribute("msg", enabled ? "Đã kích hoạt tài khoản." : "Đã khóa tài khoản.");
        return "redirect:/admin/accounts";
    }

    @PostMapping("/delete")
    @Transactional
    public String delete(@RequestParam String email, Principal principal, RedirectAttributes flash) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail.equalsIgnoreCase(principal.getName())) {
            flash.addFlashAttribute("error", "Không thể xóa tài khoản đang đăng nhập.");
            return "redirect:/admin/accounts";
        }

        User qauteUser = qauteUsers.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        UserAccount legacyUser = legacyUsers.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (qauteUser == null && legacyUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        boolean legacyAdmin = legacyUser != null && hasRole(legacyUser.getRoles(), "ADMIN");
        if ((qauteUser != null && qauteUser.getRole() == Role.ADMIN) || legacyAdmin) {
            flash.addFlashAttribute("error", "Không thể xóa tài khoản quản trị viên.");
            return "redirect:/admin/accounts";
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        if (qauteUser != null) {
            qauteUser.setDeleted(true);
            qauteUser.setDeletedEmail(qauteUser.getEmail());
            qauteUser.setEmail(deletedAccountEmails.tombstoneEmail("qaute", qauteUser.getId()));
            qauteUser.setDeletedAt(deletedAt);
            qauteUser.setDeletedBy(principal.getName());
            qauteUser.setEnabled(false);
            qauteUser.setStatus(UserStatus.SUSPENDED);
            qauteUsers.save(qauteUser);
        }
        if (legacyUser != null) {
            legacyUser.setDeleted(true);
            legacyUser.setDeletedEmail(legacyUser.getEmail());
            legacyUser.setEmail(deletedAccountEmails.tombstoneEmail("legacy", legacyUser.getId()));
            legacyUser.setDeletedAt(deletedAt);
            legacyUser.setDeletedBy(principal.getName());
            legacyUser.setEnabled(false);
            legacyUsers.save(legacyUser);
        }

        logs.log(principal.getName(), "ADMIN", "DELETE_ACCOUNT", normalizedEmail);
        flash.addFlashAttribute("msg", "Đã xóa tài khoản.");
        return "redirect:/admin/accounts";
    }

    private List<AccountRow> allAccounts() {
        Map<String, AccountAccumulator> index = new LinkedHashMap<>();
        Map<Long, AdvisorProfile> profilesByUser = new LinkedHashMap<>();
        advisorProfiles.findAll().forEach(profile -> profilesByUser.put(profile.getUser().getId(), profile));

        List<User> qauteAccounts = qauteUsers.findAll();
        List<UserAccount> legacyAccounts = legacyUsers.findAll();
        Set<String> deletedEmails = new LinkedHashSet<>();
        qauteAccounts.stream().filter(User::isDeleted).map(User::getEmail).map(this::normalize)
                .filter(email -> !email.isEmpty()).forEach(deletedEmails::add);
        legacyAccounts.stream().filter(UserAccount::isDeleted).map(UserAccount::getEmail).map(this::normalize)
                .filter(email -> !email.isEmpty()).forEach(deletedEmails::add);

        for (User user : qauteAccounts) {
            String email = normalize(user.getEmail());
            if (email.isEmpty() || deletedEmails.contains(email)) continue;
            AccountAccumulator row = index.computeIfAbsent(email, AccountAccumulator::new);
            row.addQauteUser(user);
            row.createdAt = user.getCreatedAt() == null ? row.createdAt : user.getCreatedAt();
            if (user.getRole() == Role.USER && user.isStudent()) {
                row.addRole("STUDENT", "Sinh viên");
            } else {
                row.addRole(user.getRole().name(), roleLabel(user.getRole()));
            }
            AdvisorProfile profile = profilesByUser.get(user.getId());
            if (profile != null && !profile.isDeleted()) {
                row.addRole("ADVISOR", "Tư vấn viên");
                if (profile.getAdvisorType() != null) row.addRole(profile.getAdvisorType().name(), profile.getAdvisorType().getLabel());
                if (profile.getDepartment() != null) row.organizations.add(profile.getDepartment().getName());
            }
            user.getManagedDepartments().forEach(department -> row.organizations.add(department.getName()));
        }

        for (UserAccount user : legacyAccounts) {
            String email = normalize(user.getEmail());
            if (email.isEmpty() || deletedEmails.contains(email)) continue;
            AccountAccumulator row = index.computeIfAbsent(email, AccountAccumulator::new);
            row.addLegacyUser(user);
            for (AccountRole accountRole : AccountRole.values()) {
                if (user.hasRole(accountRole)) row.addRole(accountRole.name(), roleLabel(accountRole));
            }
            if (hasRole(user.getRoles(), "ADMIN")) {
                row.addRole("ADMIN", "Quản trị viên");
            }
            if (user.getFaculty() != null) row.organizations.add(user.getFaculty().getName());
            user.getAssignedTopics().forEach(topic -> row.organizations.add(topic.getName()));
        }

        return index.values().stream().map(AccountAccumulator::toRow).toList();
    }

    private boolean isActive(User qauteUser, UserAccount legacyUser) {
        boolean qauteActive = qauteUser == null || (qauteUser.isEnabled() && qauteUser.getStatus() == UserStatus.ACTIVE);
        boolean legacyActive = legacyUser == null || legacyUser.isEnabled();
        return qauteActive && legacyActive;
    }

    private List<RoleOption> roleOptions() {
        return List.of(
                new RoleOption("ADMIN", "Quản trị viên"),
                new RoleOption("MANAGER", "Trưởng ban cố vấn"),
                new RoleOption("CONSULTANT", "Tư vấn viên"),
                new RoleOption("ADVISOR", "Tư vấn viên"),
                new RoleOption("STUDENT", "Sinh viên"),
                new RoleOption("LECTURER", "Giảng viên"),
                new RoleOption("STAFF", "Cán bộ viên chức"),
                new RoleOption("TEAM_LEADER", "Trưởng ban cố vấn"),
                new RoleOption("USER", "Người dùng"));
    }

    private String roleLabel(Role role) {
        return switch (role) {
            case ADMIN -> "Quản trị viên";
            case MANAGER -> "Trưởng ban cố vấn";
            case CONSULTANT -> "Tư vấn viên";
            case USER -> "Người dùng";
        };
    }

    private String roleLabel(AccountRole role) {
        return switch (role) {
            case STUDENT -> "Sinh viên";
            case ADVISOR -> "Tư vấn viên";
            case LECTURER -> "Giảng viên";
            case STAFF -> "Cán bộ viên chức";
            case MANAGER -> "Trưởng ban cố vấn";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasRole(String roles, String expectedRole) {
        return roles != null && Arrays.stream(roles.split(","))
                .map(String::trim).anyMatch(expectedRole::equalsIgnoreCase);
    }

    public record RoleOption(String code, String label) {}
    public record AccountRow(String email, String fullName, String roleLabels, String organizations,
                             String statusCode, String statusLabel, boolean enabled, boolean canToggle, boolean canDelete,
                             String sources, String createdAt, Set<String> roleCodes) {}

    private static final class AccountAccumulator {
        private final String email;
        private final Set<String> roles = new LinkedHashSet<>();
        private final Set<String> roleCodes = new LinkedHashSet<>();
        private final Set<String> organizations = new LinkedHashSet<>();
        private final Set<String> sources = new LinkedHashSet<>();
        private String fullName;
        private User qauteUser;
        private UserAccount legacyUser;
        private java.time.LocalDateTime createdAt;

        private AccountAccumulator(String email) { this.email = email; }
        private void addRole(String code, String label) {
            roleCodes.add(code);
            roles.add(label);
        }
        private void addQauteUser(User user) {
            qauteUser = user;
            sources.add("QAUTE");
            fullName = user.getFullName();
        }
        private void addLegacyUser(UserAccount user) {
            legacyUser = user;
            sources.add("Cổng tư vấn");
            if (fullName == null || fullName.isBlank()) fullName = user.getDisplayName();
        }
        private AccountRow toRow() {
            boolean locked = (qauteUser != null && (!qauteUser.isEnabled() || qauteUser.getStatus() == UserStatus.SUSPENDED))
                    || (legacyUser != null && !legacyUser.isEnabled());
            boolean pending = !locked && legacyUser == null && qauteUser != null && qauteUser.getStatus() == UserStatus.INACTIVE;
            String statusCode = locked ? "DISABLED" : pending ? "PENDING" : "ACTIVE";
            String statusLabel = switch (statusCode) {
                case "DISABLED" -> "Đã khóa";
                case "PENDING" -> "Chờ xác thực";
                default -> "Đang hoạt động";
            };
            boolean admin = roleCodes.contains("ADMIN");
            String date = createdAt == null ? "—" : createdAt.format(DATE_FORMAT);
            return new AccountRow(email, fullName == null || fullName.isBlank() ? email : fullName,
                    String.join(", ", roles), organizations.isEmpty() ? "—" : String.join(", ", organizations),
                    statusCode, statusLabel, statusCode.equals("ACTIVE"), !admin, !admin,
                    String.join(" + ", sources), date, Set.copyOf(roleCodes));
        }
    }
}
