package se.erik.customerservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwTService {

    private final SecretKey secretKey;
    private final long expirationMilis;

    public JwTService(@Value("${jwt.secret}")  String secret, @Value("${jwt.expiration}") long expirationMilis) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMilis = expirationMilis;
    }

    public String generateToken(Long customerId, String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMilis);

        return Jwts.builder()
                .subject(customerId.toString())
                .claim("email", email).issuedAt(now).expiration(expiration).signWith(secretKey).compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    public Long extractCustomerId(String token) {
        return Long.valueOf(extractClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);

            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

}
