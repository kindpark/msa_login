package com.example.login.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
// AI 참고했는데, 추후 어떤 역할을 하는지 공부하기
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
            String token = null;

            // 요청에서 user_token 쿠키 찾기
            if(request.getCookies() != null){
                for(Cookie cookie : request.getCookies()){
                    if("user_token".equals(cookie.getName())){
                        token = cookie.getValue();
                        break;
                    }
                }
            }

            // 토큰 검증 및 securitycontext에 인증 정보 등록
            if (token != null && jwtUtil.validateToken(token)){
                Long userId = jwtUtil.getUserIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                String authoritiyCode = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(authoritiyCode));

                // Principal 위치에 유저 PK(Id) 또는 커스텀 유저 객체를 세팅.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
    }
}
