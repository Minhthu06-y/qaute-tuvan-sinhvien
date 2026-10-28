package vn.iotstar.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

import vn.iotstar.entity.User;
import vn.iotstar.enums.Role;
import vn.iotstar.repository.UserRepository;

@Service
@Primary
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository
            .findByEmailIgnoreCase(email.trim())
            .orElseThrow(() -> new UsernameNotFoundException(
                "Không tìm thấy tài khoản."
            ));

        Role role = user.getRole();
        if (role == null) {
            throw new UsernameNotFoundException("Tài khoản chưa được gán vai trò hệ thống.");
        }
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name()));
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .authorities(authorities)
            .disabled(!user.isEnabled()
                    || user.getStatus()
                        != vn.iotstar.enums.UserStatus.ACTIVE)
            .build();
    }
}
