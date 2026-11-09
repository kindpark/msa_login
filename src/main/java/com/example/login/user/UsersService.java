package com.example.login.user;

import com.example.login.auth.AuthRepository;
import com.example.login.common.BusinessException;
import com.example.login.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    // 임시
    public boolean existsById(Long userId){
        return usersRepository.existsById(userId);
    }

    // 프론트엔드는 100개씩 나눠서 요청한다. 그보다 많으면 한 번에 너무 큰 IN 쿼리가 나가므로 막는다.
    private static final int MAX_IDS = 100;

    // 닉네임 일괄 조회: 요청한 ID 중 존재하는 사용자만 공개 정보로 돌려준다
    public List<UserResponseDto> getUsersByIds(List<Long> ids){
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > MAX_IDS) {
            throw new BusinessException(ErrorCode.TOO_MANY_IDS);
        }
        List<UserResponseDto> list = usersRepository.findAllById(ids)
                .stream()
                .map(UserResponseDto::from)
                .toList();

        return list;
    }
}
