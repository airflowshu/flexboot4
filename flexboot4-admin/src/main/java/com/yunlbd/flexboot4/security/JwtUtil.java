package com.yunlbd.flexboot4.security;

import com.yunlbd.flexboot4.auth.jwt.JwtClaimKeys;
import com.yunlbd.flexboot4.auth.jwt.JwtScopes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:thisIsASecretKeyThatIsLongEnoughForHmacSha256SecurityRequirement}")
    private String secret;

    @Value("${jwt.expiration:7200000}") // 2 hours by default as requested
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate Token
     */
    public String generateToken(UserDetails userDetails, String userId, List<String> roles, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimKeys.USERNAME, userDetails.getUsername());
        claims.put(JwtClaimKeys.ROLES, roles);
        claims.put(JwtClaimKeys.PERMISSIONS, permissions);
        claims.put(JwtClaimKeys.SCOPE, deriveScopes(roles));
        return createToken(claims, userId);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, null, java.util.Collections.emptyList(), java.util.Collections.emptyList());
    }

    private String createToken(Map<String, Object> claims, String userId) {
        String subject = (userId == null || userId.isBlank())
                ? Objects.toString(claims.get(JwtClaimKeys.USERNAME), "")
                : userId;
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private static List<String> deriveScopes(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of(JwtScopes.ADMIN);
        }
        boolean allowAi = roles.stream().anyMatch(r -> {
            String v = r == null ? "" : r.trim();
            return v.equalsIgnoreCase("AI_USER")
                    || v.equalsIgnoreCase("ROLE_AI_USER")
                    || v.equalsIgnoreCase("ADMIN")
                    || v.equalsIgnoreCase("SUPER");
        });
        return allowAi ? List.of(JwtScopes.ADMIN, JwtScopes.AI) : List.of(JwtScopes.ADMIN);
    }

    public boolean hasScope(String token, String requiredScope) {
        if (requiredScope == null || requiredScope.isBlank()) {
            return true;
        }
        Object scope = extractClaim(token, claims -> claims.get(JwtClaimKeys.SCOPE));
        if (scope == null) {
            return false;
        }
        if (scope instanceof String s) {
            return requiredScope.equals(s);
        }
        if (scope instanceof Collection<?> c) {
            for (Object it : c) {
                if (requiredScope.equals(String.valueOf(it))) {
                    return true;
                }
            }
            return false;
        }
        if (scope instanceof Object[] arr) {
            for (Object it : arr) {
                if (requiredScope.equals(String.valueOf(it))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // Try from cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        String username = extractClaim(token, claims -> claims.get(JwtClaimKeys.USERNAME, String.class));
        if (username != null && !username.isBlank()) {
            return username;
        }
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract userId from token
     */
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List extractRoles(String token) {
        return extractClaim(token, claims -> claims.get(JwtClaimKeys.ROLES, List.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public static String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
