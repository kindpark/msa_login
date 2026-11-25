package com.example.login.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    // 1. 비밀번호 암호화에 사용할 BCryptPasswordEncoder Bean 등록 (UserService 필수)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Spring Security 필터 체인 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // REST API이므로 CSRF 비활성화
                .formLogin(AbstractHttpConfigurer::disable) // 기본 로그인 폼 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT/쿠키 방식 사용시 세션 미생성
                // 2. CORS 전면 허용 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 1. CORS Preflight(OPTIONS) 요청 전면 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
// OPTIONS (Preflight) 요청 전면 허용 (CorsUtils 대신 HttpMethod.OPTIONS 사용)
                                .requestMatchers(HttpMethod.POST,
                                        "/api/v1/users/signup",
                                        "/api/v1/users/login",
                                        "/api/v1/users/logout",
                                        "/api/v1/users/find-id",
                                        "/api/v1/users/password-reset",
                                        "/api/v1/users/password-reset/confirm").permitAll()
                                // X 콜백은 로그인 쿠키 없이 온다(SameSite=Strict). 대신 state 와 x_oauth 쿠키로 확인한다.
                                // 이 주소 하나만 연다. /integrations/** 전체를 열면 안 된다
                                .requestMatchers("/internal/**",
                        "/api/v1/integrations/x/callback").permitAll()
                                .requestMatchers("/error").permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
        // 수동 필터 추가 (중복 호출이 일어나지 않도록 여기에만 직접 등록)
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)

                // 토큰 누락 및 유효하지 않은 인증(401 Unauthorized) 예외 처리 핸들러 연동
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(customAuthenticationEntryPoint));
        return http.build();
    }

    // 3. Vue.js 프론트엔드와의 통신을 위한 CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000")); // Vue 개발서버 주소
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // 쿠키/인증 헤더 전달 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}