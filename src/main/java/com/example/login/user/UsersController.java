package com.example.login.user;


import com.example.login.auth.AuthService;
import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.common.GlobalExceptionHandler;
import com.example.login.security.TokenCookieProvider;
import com.example.login.user.SignupRequestDto;
import com.example.login.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.ResponseEntity.noContent;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UsersController {
    private final UsersService usersService;
    private final TokenCookieProvider tokenCookieProvider;
    private final GlobalExceptionHandler globalExceptionHandler;
    // 1. 회원가입 API (누락되었던 부분 추가)
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(
            @Valid @RequestBody SignupRequestDto request) {
        usersService.signUp(request);
        return ResponseEntity.ok("회원가입 완료");
    }
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @Valid @RequestBody DeleteAccountRequestDto request,
            @AuthenticationPrincipal Long userId
    ){
        usersService.deleteAccount(request, userId);
        ResponseCookie cookie = tokenCookieProvider.expire();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
    // 본인 정보 확인
    @GetMapping("/me")
    public ResponseEntity<UserMeResponseDto> getMyInfo(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(usersService.getMyInfo(userId));
    }
    // 전 유저 조회
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getUsersByIds(@RequestParam(required = false) List<Long> ids){
        return ResponseEntity.ok(usersService.getUsersByIds(ids));
    }
    // 추가: 핸들로 단건 조회
    @GetMapping("/handle/{handle}")
    public ResponseEntity<UserResponseDto> getUserByHandle(@PathVariable String handle) {
        UserResponseDto response = usersService.getUserByHandle(handle);
        return ResponseEntity.ok(response);
    }
}
