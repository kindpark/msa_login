package com.example.login.auth;

import com.example.login.auth.update.UpdatePasswordRequestDto;
import com.example.login.security.JwtUtil;
import com.example.login.user.Users;
import com.example.login.user.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) //읽기 전용으로 조회 성능 향상
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final AuthRepository authRepository;
    //토큰 발급을 위한 jwtutil
    private final JwtUtil jwtUtil;

    // 아이디(이메일) 찾기
    public String findEmailByName(String name) {
        Users user = authRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("해당 이름으로 가입된 사람이 없습니다."));
        return maskEmail(user.getEmail());
    }

    // 비밀번호 재설정 (임시 비밀번호 생성)
    @Transactional
    public String resetPassword(String email, String name) {
        Users user = authRepository.findByEmailAndName(email, name)
                .orElseThrow(() -> new IllegalArgumentException("입력하신 정보와 일치하는 계정이 없습니다.")); // 세미콜론 수정

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        user.changePassword(passwordEncoder.encode(tempPassword));
        return tempPassword;
    }
    // 비밀번호 직접 변경 (마이페이지용)
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequestDto dto) {
        Users user = authRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(dto.getNewPassword()));
    }
    //이메일 마스킹 유틸 메서드
    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 3) {
            return email.substring(0, 1) + "***" + email.substring(atIndex);
        }
        return email.substring(0, 3) + "***" + email.substring(atIndex);
    }
    //로그인
    public Users login(String email, String password){
        // 1. 이메일 존재 여부 검증
        Users user = authRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 2. 비밀번호 일치 여부 검증 (평문 비밀번호 vs 암호화된 DB 비밀번호)
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user; // 검증 통과 시 유저 객체 반환
    }
}
