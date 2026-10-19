package vn.iotstar.qaute.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.iotstar.model.UserAccount;
import vn.iotstar.qaute.entity.User;
import vn.iotstar.qaute.repository.UserRepository;
import vn.iotstar.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class DeletedAccountEmailService {
    private final UserRepository qauteUsers;
    private final UserAccountRepository legacyUsers;

    @Transactional
    public void releaseForReuse(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty()) return;

        User qauteUser = qauteUsers.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        UserAccount legacyUser = legacyUsers.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if ((qauteUser != null && !qauteUser.isDeleted())
                || (legacyUser != null && !legacyUser.isDeleted())) return;

        if (qauteUser != null) {
            if (qauteUser.getDeletedEmail() == null || qauteUser.getDeletedEmail().isBlank()) {
                qauteUser.setDeletedEmail(qauteUser.getEmail());
            }
            qauteUser.setEmail(tombstoneEmail("qaute", qauteUser.getId()));
            qauteUsers.saveAndFlush(qauteUser);
        }
        if (legacyUser != null) {
            if (legacyUser.getDeletedEmail() == null || legacyUser.getDeletedEmail().isBlank()) {
                legacyUser.setDeletedEmail(legacyUser.getEmail());
            }
            legacyUser.setEmail(tombstoneEmail("legacy", legacyUser.getId()));
            legacyUsers.saveAndFlush(legacyUser);
        }
    }

    public String tombstoneEmail(String source, Long id) {
        return "deleted-" + source + "-" + id + "-" + UUID.randomUUID() + "@invalid.local";
    }
}
