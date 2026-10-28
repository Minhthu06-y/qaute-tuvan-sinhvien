package vn.iotstar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import vn.iotstar.security.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http.securityMatcher("/api", "/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/health").permitAll()
                        .requestMatchers("/api/notifications/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, error) -> response.sendError(401, "Authentication is required")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain webSecurityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http.csrf(csrf -> csrf.ignoringRequestMatchers("/presence/heartbeat")
                        .ignoringRequestMatchers((RequestMatcher) request -> {
                            String authorization = request.getHeader("Authorization");
                            return authorization != null && authorization.startsWith("Bearer ");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/register/**",
                                "/forgot-password", "/forgot-password/**", "/reset-password", "/reset-password/**", "/error",
                                "/css/**", "/js/**", "/images/**", "/webjars/**")
                        .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/manager/**").hasRole("MANAGER")
                        .requestMatchers("/report/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/notifications/**").hasRole("ADMIN")
                        .requestMatchers("/presence/**").hasRole("CONSULTANT")
                        .requestMatchers("/questions/new").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/questions").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/questions/public", "/questions/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/questions/*/answers", "/questions/*/visibility",
                                "/questions/*/transfer")
                        .hasAnyRole("CONSULTANT", "MANAGER")
                        .requestMatchers("/questions/**", "/account", "/ws/**").authenticated()
                        .requestMatchers("/logout").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, error) -> response.sendRedirect("/login")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
