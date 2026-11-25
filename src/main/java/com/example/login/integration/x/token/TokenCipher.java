package com.example.login.integration.x.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * X 토큰을 DB 에 잠가 두는 금고.  토큰 = 귀중품, secretKey = 열쇠, IV = 잠글 때마다 바뀌는 번호표.
 *  - AES : 같은 열쇠로 잠그고 연다(대칭키).  GCM : 잠그면서 봉인도 붙여, 누가 바꾸면 열 때 예외가 난다
 *  - 비밀번호는 비교만 하면 되니 되돌릴 수 없는 BCrypt, 토큰은 X 에 보내야 하니 되돌릴 수 있는 암호화
 *  - Base64 는 바이트를 글자로 적는 방법일 뿐 암호화가 아니다
 *  - 열쇠를 잃거나 바꾸면 저장된 토큰을 열 수 없다 -> X 를 다시 연결해야 한다
 */
@Component
@ConditionalOnProperty(name = "integration.x.client", havingValue = "official")
public class TokenCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12;   // bytes (권장 길이)

    private final SecretKeySpec secretKey;

    // application.yml에 설정된 32바이트(256비트) 비밀키 주입
    public TokenCipher(@Value("${integration.x.token-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        // 잘못된 키를 나중에 알지 않고 앱이 켜질 때 바로 알도록
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256을 위해 키 길이는 반드시 32바이트여야 합니다.");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    // 토큰 암호화 (DB 저장 전 호출)
    public String encrypt(String plainToken) {
        if (plainToken == null) return null;
        try {
            // 같은 키로 IV 를 재사용하면 내용이 드러나고 봉인(GCM)도 깨진다 -> 매번 새로. IV 자체는 비밀이 아니다
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv); // 매번 새로운 랜덤 IV 생성

            // Cipher 는 여러 요청이 같이 쓰면 섞인다 -> 필드에 두지 않고 매번 만든다
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));

            // 복호화할 때 IV가 필요하므로 [IV + 암호문] 형태로 합쳐서 Base64 인코딩
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("X 토큰 암호화 실패", e);
        }
    }

    // 토큰 복호화 (X API 호출 전 DB에서 꺼낸 뒤 호출)
    public String decrypt(String encryptedToken) {
        if (encryptedToken == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedToken);

            // 앞부분 12바이트(IV)와 뒷부분(실제 암호문) 분리
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            // 암호문이 바뀌었거나 열쇠가 다르면 여기서 예외 -> 아래 catch
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("X 토큰 복호화 실패", e);
        }
    }
}
