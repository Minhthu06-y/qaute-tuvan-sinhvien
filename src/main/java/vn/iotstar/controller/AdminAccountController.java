package vn.iotstar.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.iotstar.entity.AdvisorProfile;
import vn.iotstar.entity.User;
import vn.iotstar.enums.Role;
import vn.iotstar.enums.UserStatus;
import vn.iotstar.repository.AdvisorProfileRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.ActivityLogService;
import vn.iotstar.service.DeletedAccountEmailService;

@Controller
@RequestMapping("/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAccountController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository users;
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
        List<AccountRow> allAccounts = users.findAll().stream()
                .filter(user -> !user.isDeleted())
                .map(this::toRow)
                .toList();
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
        String normalizedEmail = normalize(email);
        if (normalizedEmail.equalsIgnoreCase(principal.getName())) {
            flash.addFlashAttribute("error", "Không thể khóa tài khoản quản trị viên đang đăng nhập.");
            return "redirect:/admin/accounts";
        }
        User user = users.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getRole() == Role.ADMIN) {
            flash.addFlashAttribute("error", "Không thể khóa tài khoản quản trị viên.");
            return "redirect:/admin/accounts";
        }

        user.setEnabled(!user.isEnabled());
        user.setStatus(user.isEnabled() ? UserStatus.ACTIVE : UserStatus.SUSPENDED);
        users.save(user);
        logs.log(principal.getName(), "ADMIN", user.isEnabled() ? "ENABLE_ACCOUNT" : "DISABLE_ACCOUNT", normalizedEmail);
        flash.addFlashAttribute("msg", user.isEnabled() ? "Đã kích hoạt tài khoản." : "Đã khóa tài khoản.");
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
        User user = users.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getRole() == Role.ADMIN) {
            flash.addFlashAttribute("error", "Không thể xóa tài khoản quản trị viên.");
            return "redirect:/admin/accounts";
        }

        user.setDeleted(true);
        user.setDeletedEmail(user.getEmail());
        user.setEmail(deletedAccountEmails.tombstoneEmail(user.getId()));
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(principal.getName());
        user.setEnabled(false);
        user.setStatus(UserStatus.SUSPENDED);
        users.save(user);
        logs.log(principal.getName(), "ADMIN", "DELETE_ACCOUNT", normalizedEmail);
        flash.addFlashAttribute("msg", "Đã xóa tài khoản.");
        return "redirect:/admin/accounts";
    }

    private AccountRow toRow(User user) {
        Set<String> roleCodes = new LinkedHashSet<>();
        Set<String> roleLabels = new LinkedHashSet<>();
        addRole(roleCodes, roleLabels, user.getRole().name(), roleLabel(user.getRole()));

        Set<String> organizations = new LinkedHashSet<>();
        if (user.getFaculty() != null) organizations.add(user.getFaculty().getName());
        user.getAssignedTopics().forEach(topic -> organizations.add(topic.getName()));
        user.getManagedDepartments().forEach(department -> organizations.add(department.getName()));
        AdvisorProfile profile = advisorProfiles.findByUserId(user.getId()).orElse(null);
        if (profile != null && !profile.isDeleted()) {
            if (profile.getDepartment() != null) organizations.add(profile.getDepartment().getName());
        }

        boolean locked = !user.isEnabled() || user.getStatus() == UserStatus.SUSPENDED;
        boolean pending = !locked && user.getStatus() == UserStatus.INACTIVE;
        String statusCode = locked ? "DISABLED" : pending ? "PENDING" : "ACTIVE";
        String statusLabel = switch (statusCode) {
            case "DISABLED" -> "Đã khóa";
            case "PENDING" -> "Chờ xác thực";
            default -> "Đang hoạt động";
        };
        String createdAt = user.getCreatedAt() == null ? "—" : user.getCreatedAt().format(DATE_FORMAT);
        return new AccountRow(user.getEmail(), user.getFullName(), String.join(", ", roleLabels),
                organizations.isEmpty() ? "—" : String.join(", ", organizations), statusCode, statusLabel,
                statusCode.equals("ACTIVE"), !roleCodes.contains("ADMIN"), !roleCodes.contains("ADMIN"),
                "QAUTE", createdAt, Set.copyOf(roleCodes));
    }

    private void addRole(Set<String> codes, Set<String> labels, String code, String label) {
        codes.add(code);
        labels.add(label);
    }

    private List<RoleOption> roleOptions() {
        return List.of(
                new RoleOption("ADMIN", "Quản trị viên"),
                new RoleOption("CONSULTANT", "Tư vấn viên"),
                new RoleOption("MANAGER", "Quản lý khoa"),
                new RoleOption("USER", "Người dùng"));
    }

    private String roleLabel(Role role) {
        return switch (role) {
            case ADMIN -> "Quản trị viên";
            case MANAGER -> "Quản lý khoa";
            case CONSULTANT -> "Tư vấn viên";
            case USER -> "Người dùng";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record RoleOption(String code, String label) {}
    public record AccountRow(String email, String fullName, String roleLabels, String organizations,
                             String statusCode, String statusLabel, boolean enabled, boolean canToggle, boolean canDelete,
                             String sources, String createdAt, Set<String> roleCodes) {}
}
