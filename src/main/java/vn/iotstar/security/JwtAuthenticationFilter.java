package vn.iotstar.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import vn.iotstar.enums.UserStatus;
import vn.iotstar.entity.User;
import vn.iotstar.enums.Role;
import vn.iotstar.repository.UserRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                String email = jwtService.subject(token);
                User user = users.findByEmailIgnoreCase(email).orElse(null);
                if (user == null || !user.isEnabled() || user.isDeleted()
                        || user.getStatus() != UserStatus.ACTIVE || user.getRole() == null) {
                    SecurityContextHolder.clearContext();
                } else {
                    // The database is the source of truth so role changes apply to existing tokens immediately.
                    Role role = user.getRole();
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role.name()));
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(email, null, authorities));
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        } else {
            // A web session can outlive an account merge, disable, or role change.
            // Rebuild its authority from the current user row before URL authorization runs.
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)) {
                User user = users.findByEmailIgnoreCase(authentication.getName()).orElse(null);
                if (user == null || !user.isEnabled() || user.isDeleted()
                        || user.getStatus() != UserStatus.ACTIVE || user.getRole() == null) {
                    SecurityContextHolder.clearContext();
                } else {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
                }
            }
        }
        chain.doFilter(request, response);
    }
}
