package vn.iotstar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.iotstar.entity.*;
import vn.iotstar.enums.*;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByDeletedEmailIgnoreCase(String deletedEmail);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRole(Role role);
}

