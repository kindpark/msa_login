package com.example.login.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 에러의 단일 목록. 응답은 항상 { "code", "message", "status" } 형식으로 나간다.
 * code 는 프론트엔드가 분기할 때 쓰는 값이라 enum 이름과 같게 맞춘다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 BAD REQUEST
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력값이 올바르지 않습니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "현재 비밀번호가 일치하지 않습니다."),
    SELF_FOLLOW(HttpStatus.BAD_REQUEST, "SELF_FOLLOW", "자기 자신을 팔로우할 수 없습니다."),

    // 401 UNAUTHORIZED
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요하거나 인증 자격 증명이 유효하지 않습니다."),

    // 404 NOT FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 회원입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 경로를 찾을 수 없습니다."),

    // 409 CONFLICT
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 존재하는 데이터입니다."),

    // 500 INTERNAL SERVER ERROR
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
