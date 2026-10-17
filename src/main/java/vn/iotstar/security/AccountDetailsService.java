package vn.iotstar.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.iotstar.repository.UserAccountRepository;

@Service
public class AccountDetailsService implements UserDetailsService {
    private final UserAccountRepository users;
    public AccountDetailsService(UserAccountRepository users) { this.users = users; }

    @Override public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var account = users.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        List<SimpleGrantedAuthority> authorities = Arrays.stream(account.getRoles().split(","))
                .map(String::trim).filter(role -> !role.isBlank())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        return User.withUsername(account.getEmail()).password(account.getPasswordHash()).authorities(authorities).build();
    }
}
