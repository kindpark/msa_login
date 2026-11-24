package com.example.login.auth;

import com.example.login.auth.update.UpdatePasswordRequestDto;
import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import com.example.login.security.JwtUtil;
import com.example.login.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    //리스트로 받는 이유: 결과가 여러건일 수도 있다. 우리는 이것 중 정확히 하나만 찾는다.
    // 빈 닉네임은 FindIdRequestDto 의 @NotBlank 가 컨트롤러 단계에서 막는다
    public List<String> findEmailsByNickname(String nickname) {
        return authRepository.findByNickname(nickname).stream()
                .map(u->maskEmail(u.getEmail())).toList();
    }

    // 비밀번호 재설정 (임시 비밀번호 생성)
    @Transactional
    public String resetPassword(String email, String nickname) {
        Users user = authRepository.findByEmailAndNickname(email, nickname)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        user.changePassword(passwordEncoder.encode(tempPassword));
        return tempPassword;
    }
    // 비밀번호 직접 변경 (마이페이지용)
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequestDto dto) {
        Users user = authRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
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
    // 이메일이 없는 경우와 비밀번호가 틀린 경우를 같은 에러로 돌려준다.
    // 메시지가 다르면 어떤 이메일이 가입되어 있는지 알아낼 수 있기 때문이다.
    public Users login(String email, String password){
        // 1. 이메일 존재 여부 검증
        Users user = authRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 2. 비밀번호 일치 여부 검증 (평문 비밀번호 vs 암호화된 DB 비밀번호)
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return user; // 검증 통과 시 유저 객체 반환
    }
}
