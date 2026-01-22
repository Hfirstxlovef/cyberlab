package org.cyberlab.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.cyberlab.entity.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    // ≥ 32 chars 的秘钥，不能太短
    private final String secret = "a-very-long-and-secure-secret-key-for-jwt-token-generation-123456";
    private SecretKey key;
    private final long EXPIRATION = 15 * 60 * 1000L; // 15 minutes
    private final long REFRESH_EXPIRATION = 7 * 24 * 60 * 60 * 1000L; // 7 days for refresh token

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ✅ 生成 JWT，authorities 使用 List<String>
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("authorities", List.of("ROLE_" + user.getRole()))  // ⬅️ 用数组形式存储
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ 获取用户名
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // ✅ 获取权限列表
    @SuppressWarnings("unchecked")
    public List<String> getAuthorities(String token) {
        return parseClaims(token).get("authorities", List.class);
    }

    // ✅ 验证 JWT
    public boolean validateToken(String token) {
        try {
            parseClaims(token); // 会抛出异常表示无效
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Debug statement removed
            return false;
        }
    }

    // ✅ 生成刷新令牌
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("type", "refresh")  // 标识为刷新令牌
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ 验证刷新令牌
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Claims claims = parseClaims(refreshToken);
            String tokenType = claims.get("type", String.class);
            return "refresh".equals(tokenType);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ✅ 从刷新令牌获取用户名
    public String getUsernameFromRefreshToken(String refreshToken) {
        if (!validateRefreshToken(refreshToken)) {
            return null;
        }
        return parseClaims(refreshToken).getSubject();
    }

    // ✅ 检查令牌是否即将过期（5分钟内过期）
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            long timeToExpire = expiration.getTime() - System.currentTimeMillis();
            return timeToExpire < 5 * 60 * 1000L; // 5 minutes
        } catch (JwtException | IllegalArgumentException e) {
            return true; // 如果解析失败，认为需要刷新
        }
    }

    // ✅ 获取令牌剩余有效时间（毫秒）
    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            return Math.max(0, expiration.getTime() - System.currentTimeMillis());
        } catch (JwtException | IllegalArgumentException e) {
            return 0;
        }
    }

    // ✅ 获取令牌过期时间
    public long getTokenExpirationTime() {
        return EXPIRATION;
    }

    // ✅ 私有方法：解析 Token 的 Claims
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}