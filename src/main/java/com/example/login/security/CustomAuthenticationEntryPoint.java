package com.example.login.security;

import com.example.login.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component // SecurityConfig에서 생성자 주입(DI)을 받을 수 있도록 컴포넌트로 등록합니다.
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // 컨트롤러 밖(보안 필터)에서 일어나는 401 이라 GlobalExceptionHandler 를 거치지 않는다.
        // 대신 같은 ErrorCode 값을 써서 다른 에러와 형식·문구를 맞춘다.
        ErrorCode error = ErrorCode.UNAUTHORIZED;
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(error.getHttpStatus().value());
        response.getWriter().write(String.format(
                "{\"code\":\"%s\",\"message\":\"%s\",\"status\":%d}",
                error.getCode(), error.getMessage(), error.getHttpStatus().value()));
    }
}