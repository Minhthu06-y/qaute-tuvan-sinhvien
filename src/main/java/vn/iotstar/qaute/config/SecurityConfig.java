package vn.iotstar.qaute.config;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * BẢN TẠM để chạy/test độc lập phần Admin–Manager (đăng nhập form).
 * Khi ghép với Người 1: giữ NGUYÊN các quy tắc URL bên dưới, thay formLogin bằng JwtAuthenticationFilter.
 * Phòng thủ 2 lớp: URL rule ở đây + @PreAuthorize trên từng controller + kiểm tra phạm vi khoa ở service.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
                .requestMatchers("/css/**", "/js/**", "/img/**", "/error").permitAll()
                // Khách chưa đăng nhập: CHỈ ĐỌC trang công khai (đường dẫn do Người 1 quyết định, chỉnh lại cho khớp)
                .requestMatchers(HttpMethod.GET, "/", "/login", "/register", "/forgot-password", "/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasRole("MANAGER")
                .requestMatchers("/report/**").hasAnyRole("ADMIN", "MANAGER")
                .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .logout(Customizer.withDefaults());
        return http.build();
    }
}

