package com.example.login.integration.x;


import lombok.Getter;
import java.util.Arrays;

@Getter
public enum Provider {
    X("X"),
    BLUESKY("BlueSky");

    private final String provider;

    Provider(String provider) {
        this.provider = provider;
    }

    // 케이스 A: 입력값이 유효한지 검사한 뒤 정제된 String("X" 또는 "BlueSky") 반환
    public static String resolveName(String input) {
        return Arrays.stream(values())
                .filter(p -> p.provider.equalsIgnoreCase(input))
                .findFirst()
                .map(Provider::getProvider)
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 프로바이더입니다: " + input));
    }
}
