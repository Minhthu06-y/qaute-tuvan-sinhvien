package vn.iotstar.qaute.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import vn.iotstar.qaute.entity.User;
import vn.iotstar.qaute.repository.UserRepository;

@Service
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
                        != vn.iotstar.qaute.enums.UserStatus.ACTIVE)
            .build();
    }
}