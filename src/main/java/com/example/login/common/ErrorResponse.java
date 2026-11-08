package com.example.login.common;

/** 모든 에러 응답의 공통 형식: { "code", "message", "status" } */
public record ErrorResponse(String code, String message, int status) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus().value());
    }

    /** 같은 코드에 더 구체적인 메시지를 쓰고 싶을 때 (예: 어떤 입력값이 틀렸는지) */
    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message, errorCode.getHttpStatus().value());
    }
}
