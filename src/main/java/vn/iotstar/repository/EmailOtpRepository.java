package vn.iotstar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.iotstar.entity.EmailOtp;


public interface EmailOtpRepository
        extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByEmailAndUsedFalseOrderByIdDesc(
            String email);
}