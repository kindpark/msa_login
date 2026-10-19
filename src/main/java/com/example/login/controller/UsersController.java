package com.example.login.controller;


import com.example.login.config.JwtUtil;
import com.example.login.dto.AdminLoginRequest;
import com.example.login.entity.Users;
import com.example.login.repository.UsersRepository;
import com.example.login.service.UsersService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UsersController {
    private final UsersRepository usersRepository;
    private final UsersService usersService;
    private final JwtUtil jwtUtil;
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginRequest request, HttpServletResponse response){
        Users users = usersService.login(request.getEmail(), request.getPassword());
        String token = jwtUtil.generateToken("user");
        ResponseCookie cookie = ResponseCookie.from("user_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(2*60*60)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok("로그인 성공");
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response){
        ResponseCookie cookie = ResponseCookie.from("user_token")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok("로그아웃 성공");
    }
}
