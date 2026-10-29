package vn.iotstar.service;

import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.iotstar.entity.User;
import vn.iotstar.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class DeletedAccountEmailService {
    private final UserRepository users;

    @Transactional
    public void releaseForReuse(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty()) return;

        Map<Long, User> deletedAccounts = new LinkedHashMap<>();
        users.findByEmailIgnoreCase(normalizedEmail)
                .filter(User::isDeleted)
                .ifPresent(user -> deletedAccounts.put(user.getId(), user));
        users.findAllByDeletedEmailIgnoreCase(normalizedEmail).stream()
                .filter(User::isDeleted)
                .forEach(user -> deletedAccounts.putIfAbsent(user.getId(), user));

        if (deletedAccounts.isEmpty()) return;

        for (User user : deletedAccounts.values()) {
            if (user.getDeletedEmail() == null || user.getDeletedEmail().isBlank()) {
                user.setDeletedEmail(user.getEmail());
            }
            user.setEmail(tombstoneEmail(user.getId()));
        }
        users.saveAllAndFlush(deletedAccounts.values());
    }

    public String tombstoneEmail(String source, Long id) {
        return tombstoneEmail(id);
    }

    public String tombstoneEmail(Long id) {
        return "deleted-" + id + "-" + UUID.randomUUID() + "@invalid.local";
    }
}
