package com.example.login.user;

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
    public Long signUp(SignupRequestDto request){
        if(usersRepository.existsByEmail(request.getEmail())){
            throw new IllegalArgumentException("가입하려는 이메일이 존재하는 이메일입니다.");

        }
        Users user = Users.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role("USER")
                .build();

        Users savedUser = usersRepository.save(user);
        return savedUser.getId();
    }
}
