package com.example.login.common;

import lombok.Getter;

/**
 * 서비스에서 예상한 실패(로그인 실패, 중복 이메일 등)를 알릴 때 던지는 예외.
 * GlobalExceptionHandler 가 ErrorCode 에 맞는 상태 코드와 메시지로 바꿔 응답한다.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
