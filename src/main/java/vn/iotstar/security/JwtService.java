package vn.iotstar.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT secret must be at least 256 bits");
        this.key = Keys.hmacShaKeyFor(bytes); this.expirationMs = expirationMs;
    }

    public String issue(String email, String roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder().subject(email).claim("roles", roles).issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs)).signWith(key).compact();
    }
    public String subject(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject(); }
    public String roles(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("roles", String.class); }
}
