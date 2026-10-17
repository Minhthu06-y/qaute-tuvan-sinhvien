package vn.iotstar.user.repository;

import vn.iotstar.user.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByEmailIgnoreCase(String email);

    void deleteByEmailIgnoreCase(String email);
}