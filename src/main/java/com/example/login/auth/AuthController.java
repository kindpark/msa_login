package com.example.login.auth;

import com.example.login.auth.find.FindIdRequestDto;
import com.example.login.auth.find.FindIdResponseDto;
import com.example.login.auth.find.FindPasswordRequestDto;
import com.example.login.auth.find.FindPasswordResponseDto;
import com.example.login.auth.update.UpdatePasswordRequestDto;
import com.example.login.security.JwtUtil;
import com.example.login.user.Users;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    @Value("${cookie.secure:false}")
    private boolean cookieSecure;
    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestBody LoginRequestDto request,
            HttpServletResponse response){
        Users user = authService.login(request.getEmail(), request.getPassword());
        String token = jwtUtil.generateToken(
                        user.getId(),
                        user.getNickname(),
                        user.getRole());
        ResponseCookie cookie = ResponseCookie.from("user_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(2*60*60)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }
    //로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response){
        ResponseCookie cookie = ResponseCookie.from("user_token")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }
    // 아이디(이메일) 찾기
    @PostMapping("/find-id")
    public ResponseEntity<FindIdResponseDto> findId(@RequestBody FindIdRequestDto requestDto) {
        String maskedEmail = authService.findEmailByName(requestDto.getName());
        return ResponseEntity.ok(new FindIdResponseDto(maskedEmail, requestDto.getName()));
    }

    // 비밀번호 재설정
    @PostMapping("/find-password")
    public ResponseEntity<FindPasswordResponseDto> findPassword(@RequestBody FindPasswordRequestDto requestDto) {
        String tempPassword = authService.resetPassword(requestDto.getEmail(), requestDto.getName());
        return ResponseEntity.ok(new FindPasswordResponseDto(tempPassword));
    }
    //비번 직접 변경
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @RequestParam Long userId, // JWT에서 사용자 PK 추출
            @RequestBody UpdatePasswordRequestDto requestDto) {

        authService.updatePassword(userId, requestDto);
        return ResponseEntity.ok().build();
    }
}
