package vn.iotstar.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.iotstar.qaute.enums.UserStatus;
import vn.iotstar.qaute.repository.UserRepository;
import vn.iotstar.repository.UserAccountRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserAccountRepository users;
    private final UserRepository qauteUsers;
    public JwtAuthenticationFilter(JwtService jwtService, UserAccountRepository users, UserRepository qauteUsers) {
        this.jwtService = jwtService;
        this.users = users;
        this.qauteUsers = qauteUsers;
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                String email = jwtService.subject(token);
                boolean legacyDisabled = users.findByEmailIgnoreCase(email)
                        .filter(account -> !account.isEnabled() || account.isDeleted()).isPresent();
                boolean qauteDisabled = qauteUsers.findByEmailIgnoreCase(email)
                        .filter(account -> !account.isEnabled() || account.isDeleted()
                                || account.getStatus() != UserStatus.ACTIVE).isPresent();
                if (legacyDisabled || qauteDisabled) {
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }
                String roles = jwtService.roles(token);
                List<SimpleGrantedAuthority> authorities = Arrays.stream(roles == null ? new String[0] : roles.split(","))
                        .map(String::trim).filter(role -> !role.isBlank()).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(Collectors.toList());
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null, authorities));
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
