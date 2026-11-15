package com.example.login.auth;

import com.example.login.auth.find.FindIdRequestDto;
import com.example.login.auth.find.FindIdResponseDto;
import com.example.login.auth.update.UpdatePasswordRequestDto;
import com.example.login.security.JwtUtil;
import com.example.login.security.TokenCookieProvider;
import com.example.login.user.Users;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TokenCookieProvider tokenCookieProvider;
    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse response){
        Users user = authService.login(request.getEmail(), request.getPassword());
        String token = jwtUtil.generateToken(
                        user.getId(),
                        user.getNickname(),
                        user.getRole());
        ResponseCookie cookie = tokenCookieProvider.create(token);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }
    //로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response){
        ResponseCookie cookie = tokenCookieProvider.expire();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }
    // 아이디(이메일) 찾기
    @PostMapping("/find-id")
    public ResponseEntity<FindIdResponseDto> findEmailsByNickname(@Valid @RequestBody FindIdRequestDto requestDto) {
        List<String> emails = authService.
                findEmailsByNickname(requestDto.getNickname());
        return ResponseEntity.ok(
                new FindIdResponseDto(emails));
    }
/*
    // 비밀번호 재설정
    @PostMapping("/find-password")
    public ResponseEntity<FindPasswordResponseDto> resetPassword(@RequestBody FindPasswordRequestDto requestDto) {
        String tempPassword = authService.resetPassword(requestDto.getEmail(), requestDto.getNickname());
        return ResponseEntity.ok(new FindPasswordResponseDto(tempPassword));
    }

 */
    //비번 직접 변경
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal Long userId, // JWT에서 사용자 PK 추출
            @Valid @RequestBody UpdatePasswordRequestDto requestDto) {

        authService.updatePassword(userId, requestDto);
        return ResponseEntity.ok().build();
    }
}
