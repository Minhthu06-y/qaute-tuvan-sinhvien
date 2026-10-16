package vn.iotstar.qaute.service;

import vn.iotstar.qaute.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

/** Bản tạm cho đăng nhập form; Người 1 thay bằng JWT. Role USER/ADMIN/MANAGER -> ROLE_*. */
@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {
    private final UserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String email) {
        var u = repo.findByEmailIgnoreCase(email).orElseThrow(() -> new UsernameNotFoundException(email));
        return org.springframework.security.core.userdetails.User.withUsername(u.getEmail())
                .password(u.getPasswordHash()).roles(u.getRole().name()).disabled(!u.isEnabled()).build();
    }
}

