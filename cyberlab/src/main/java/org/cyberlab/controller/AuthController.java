package org.cyberlab.controller;

import org.cyberlab.dto.AuthRequest;
import org.cyberlab.dto.AuthResponse;
import org.cyberlab.entity.User;
import org.cyberlab.repository.UserRepository;
import org.cyberlab.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"https://localhost:5443", "https://127.0.0.1:5443"}, allowCredentials = "true")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;  // ✅ 正确注入 Spring Security 的 PasswordEncoder

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // Debug statement removed
        // Debug statement removed
        // Debug statement removed
        // Debug statement removed
        
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("用户名或密码错误");
        }

        String token = jwtProvider.generateToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);
        // Debug statement removed

        // 设置 httpOnly Cookie
        Cookie tokenCookie = new Cookie("accessToken", token);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(true); // HTTPS 环境必须为 true
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(15 * 60); // 15分钟，与JWT有效期一致
        httpResponse.addCookie(tokenCookie);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // HTTPS 环境必须为 true
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7天
        httpResponse.addCookie(refreshTokenCookie);

        // 同时返回令牌给前端（向后兼容），包含用户ID
        return ResponseEntity.ok(new AuthResponse(token, refreshToken, user.getUsername(), user.getRole(), user.getId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader(value = "Authorization", required = false) String authHeader, 
                                        HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 从请求头或Cookie获取 refresh token
        String refreshToken = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            refreshToken = authHeader.substring(7);
        } else {
            // 从Cookie获取刷新令牌
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }
        }
        
        if (refreshToken == null) {
            return ResponseEntity.status(401).body("缺少有效的刷新令牌");
        }
        
        // 验证刷新令牌
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).body("刷新令牌无效或已过期");
        }
        
        // 获取用户名
        String username = jwtProvider.getUsernameFromRefreshToken(refreshToken);
        if (username == null) {
            return ResponseEntity.status(401).body("无法从刷新令牌获取用户信息");
        }
        
        // 查找用户
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body("用户不存在");
        }
        
        // 生成新的访问令牌和刷新令牌
        String newToken = jwtProvider.generateToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);
        
        // 更新Cookie
        Cookie tokenCookie = new Cookie("accessToken", newToken);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(true); // HTTPS 环境必须为 true
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(15 * 60); // 15分钟
        httpResponse.addCookie(tokenCookie);

        Cookie refreshTokenCookie = new Cookie("refreshToken", newRefreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // HTTPS 环境必须为 true
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7天
        httpResponse.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(new AuthResponse(newToken, newRefreshToken, user.getUsername(), user.getRole(), user.getId()));
    }
}