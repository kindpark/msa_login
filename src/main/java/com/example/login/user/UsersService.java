package com.example.login.user;

import com.example.login.auth.AuthRepository;
import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) //읽기 전용으로 조회 성능 향상
public class UsersService {
    private final UsersRepository usersRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    @Transactional
    public Long signUp(SignupRequestDto request){
        if(usersRepository.existsByEmail(request.getEmail())){
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        // 위 확인과 저장 사이에 같은 이메일로 동시에 가입하면 DB 의 unique 제약이 막고,
        // GlobalExceptionHandler 가 409 로 응답한다.
        Users user = Users.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role("USER")
                .build();

        Users savedUser = usersRepository.save(user);
        return savedUser.getId();
    }
    public UserMeResponseDto getMyInfo(Long userId) {
        Users user = authRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new UserMeResponseDto(user.getId(), user.getEmail(), user.getNickname(), user.getRole());
    }
}
