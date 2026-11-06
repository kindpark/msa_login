package com.example.login.user;


import com.example.login.user.SignupRequestDto;
import com.example.login.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<String> signup(
            @RequestBody SignupRequestDto request) {
        usersService.signUp(request);
        return ResponseEntity.ok("회원가입 완료");
    }
}
