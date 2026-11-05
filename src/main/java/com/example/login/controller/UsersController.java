package com.example.login.controller;


import com.example.login.config.JwtUtil;
import com.example.login.dto.*;
import com.example.login.entity.Users;
import com.example.login.repository.UsersRepository;
import com.example.login.service.UsersService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsersController {
    private final UsersRepository usersRepository;
    private final UsersService usersService;
    private final JwtUtil jwtUtil;
    // 1. 회원가입 API (누락되었던 부분 추가)
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody UserSignupRequest request) {
        usersService.signUp(request);
        return ResponseEntity.ok("회원가입 완료");
    }

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
    //로그아웃
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
    // 아이디(이메일) 찾기
    @PostMapping("/find-id")
    public ResponseEntity<FindIdResponseDto> findId(@RequestBody FindIdRequestDto requestDto) {
        String maskedEmail = usersService.findEmailByName(requestDto.getName());
        return ResponseEntity.ok(new FindIdResponseDto(maskedEmail));
    }

    // 비밀번호 재설정
    @PostMapping("/find-password")
    public ResponseEntity<FindPasswordResponseDto> findPassword(@RequestBody FindPasswordRequestDto requestDto) {
        String tempPassword = usersService.resetPassword(requestDto.getEmail(), requestDto.getName());
        return ResponseEntity.ok(new FindPasswordResponseDto(tempPassword));
    }
    //비번 직접 변경
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @RequestParam Long userId, // JWT에서 사용자 PK 추출
            @RequestBody UpdatePasswordRequestDto requestDto) {

        usersService.updatePassword(userId, requestDto);
        return ResponseEntity.ok().build();
    }
}
