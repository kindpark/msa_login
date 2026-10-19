package com.example.login.service;

import com.example.login.dto.AdminLoginRequest;
import com.example.login.dto.UserSignupRequest;
import com.example.login.entity.Users;
import com.example.login.repository.UsersRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) //읽기 전용으로 조회 성능 향상
public class UsersService {
    private final UsersRepository usersRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signUp(UserSignupRequest request){
        if(usersRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("가입하려는 이메일이 존재하는 이메일입니다.");

        }
        Users user = Users.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role("USER")
                .build();

        Users savedUser = usersRepository.save(user);
        return savedUser.getId();
    }
    //로그인
    public Users login(String email, String password){
        // 1. 이메일 존재 여부 검증
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 2. 비밀번호 일치 여부 검증 (평문 비밀번호 vs 암호화된 DB 비밀번호)
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user; // 검증 통과 시 유저 객체 반환
    }
}
