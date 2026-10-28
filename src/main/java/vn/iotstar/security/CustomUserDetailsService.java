package vn.iotstar.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

import vn.iotstar.entity.User;
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

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .authorities("ROLE_" + user.getRole().name())
            .disabled(!user.isEnabled()
                    || user.getStatus()
                        != vn.iotstar.enums.UserStatus.ACTIVE)
            .build();
    }
}
