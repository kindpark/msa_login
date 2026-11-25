package com.example.login.integration.x.dto;

// verify 를 통과한 결과. userId: 누구의 연결인지, codeVerifier: 토큰 교환 때 X 에 내는 원본
public record Verified(Long userId, String codeVerifier) {
}
