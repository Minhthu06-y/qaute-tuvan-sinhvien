package vn.iotstar.service;

import java.util.Locale;
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

        User user = users.findByEmailIgnoreCase(normalizedEmail)
                .or(() -> users.findByDeletedEmailIgnoreCase(normalizedEmail))
                .orElse(null);
        if (user == null || !user.isDeleted()) return;

        if (user.getDeletedEmail() == null || user.getDeletedEmail().isBlank()) {
            user.setDeletedEmail(user.getEmail());
        }
        user.setEmail(tombstoneEmail(user.getId()));
        users.saveAndFlush(user);
    }

    public String tombstoneEmail(String source, Long id) {
        return tombstoneEmail(id);
    }

    public String tombstoneEmail(Long id) {
        return "deleted-" + id + "-" + UUID.randomUUID() + "@invalid.local";
    }
}
