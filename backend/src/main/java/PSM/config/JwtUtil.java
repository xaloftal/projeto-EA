package PSM.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final SecretKey secretKey;
    private final long jwtExpirationMs;
    private final StringRedisTemplate redisTemplate;

    public JwtUtil(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs) {
        
        // Ensure key is at least 256 bits (32 bytes)
        byte[] decodedKey = secret.getBytes();
        if (decodedKey.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(decodedKey, 0, paddedKey, 0, decodedKey.length);
            decodedKey = paddedKey;
        }
        
        this.secretKey = Keys.hmacShaKeyFor(decodedKey);
        this.jwtExpirationMs = expirationMs;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate JWT token for user
     */
    public String generateToken(UUID userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract user ID from token
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Check if token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Validate token
     */
    public Boolean validateToken(String token) {
        try {
            if (isTokenBlacklisted(token)) {
                return false;
            }

            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get all claims from token
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get token expiration time in milliseconds (for Redis TTL)
     */
    public long getTokenExpirationTime(String token) {
        Claims claims = getAllClaimsFromToken(token);
        long expirationTime = claims.getExpiration().getTime();
        long currentTime = System.currentTimeMillis();
        return Math.max(0, (expirationTime - currentTime) / 1000);
    }

    public String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public void revokeToken(String token) {
        long ttlSeconds = getTokenExpirationTime(token);
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "revoked", Duration.ofSeconds(ttlSeconds));
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
