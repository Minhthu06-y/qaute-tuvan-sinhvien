package vn.iotstar.user.service;

import vn.iotstar.user.entity.User;
import vn.iotstar.user.entity.Role;
import vn.iotstar.user.repository.UserRepository;

import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmailIgnoreCase(email.trim())
            .orElseThrow(() ->
                new UsernameNotFoundException("Không tìm thấy tài khoản"));

        var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();

        authorities.add(new SimpleGrantedAuthority(
            "ROLE_" + user.getRole().name()));

        // Mọi tài khoản đã xác thực đều có thể hỏi.
        authorities.add(new SimpleGrantedAuthority("QUESTION_WRITE"));

        // Chỉ tài khoản đang hoạt động mới có quyền tư vấn.
        if (user.isActive() && user.getRole().name().equals("CONSULTANT")) {
            authorities.add(new SimpleGrantedAuthority("CONSULTANT_ACTIVE"));
        }

//        return User.withUsername(user.getEmail())
//            .password(user.getPassword())
//            .authorities(authorities)
//            .disabled(!user.isEnabled())
//            .accountLocked(!user.isActive())
//            .build();
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(!user.isEnabled())
                .accountLocked(!user.isActive())
                .build();
    }
}