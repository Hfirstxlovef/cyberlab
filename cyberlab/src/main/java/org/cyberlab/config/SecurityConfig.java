package org.cyberlab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // 公开访问的API
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/range/**").permitAll()
                .requestMatchers("/api/drills/**").permitAll()
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/api/host-nodes/health-check/**").permitAll()
                .requestMatchers("/api/assets/export/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                // 系统设置读取接口允许所有人访问
                .requestMatchers(HttpMethod.GET, "/api/settings").permitAll()
                .requestMatchers("/api/settings/public").permitAll()
                .requestMatchers("/api/settings/license/validate").permitAll()
                // 成果相关接口允许认证用户访问（不限角色）
                .requestMatchers("/api/achievements/**").permitAll()
                // 其他API需要认证
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(
                org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter(), 
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public org.cyberlab.security.JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new org.cyberlab.security.JwtAuthenticationFilter();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://localhost:5443", "https://127.0.0.1:5443"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}