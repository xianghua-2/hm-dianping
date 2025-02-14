package com.hmdp.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;

import javax.crypto.SecretKey;

public class JwtUtilTest {

    // 密钥
    private static final String SECRET_KEY = "ThisIsA32BytesLongSecretKeyForHS256";

    private Map<String,Object> sampleClaims;

    @Before
    public void setUp() {
        sampleClaims = new HashMap<>();
        sampleClaims.put("userId", 1001);
        sampleClaims.put("username", "testUser");
    }

    // 正常场景测试
    @Test
    public void testCreateJWT() {
        String token = JwtUtil.createJWT(SECRET_KEY, 3600000L, sampleClaims);

        Claims claims = JwtUtil.parseJWT(SECRET_KEY, token);
        System.out.println(claims);
        // 断言验证
        Assertions.assertEquals(1001, claims.get("userId"));
        Assertions.assertEquals("testUser", claims.get("username"));
        Assertions.assertNotNull(claims.getExpiration());
    }

    // 异常场景：过期Token测试
    @Test
    public void should_throw_expired_exception() throws InterruptedException {
        // 生成1毫秒过期的Token
        String token = JwtUtil.createJWT(SECRET_KEY, 1L, sampleClaims);
        Thread.sleep(2); // 确保过期

        Assertions.assertThrows(ExpiredJwtException.class,
                () -> JwtUtil.parseJWT(SECRET_KEY, token));
    }

    // 异常场景：签名密钥错误测试
    @Test
    public void should_throw_signature_exception() {
        String token = JwtUtil.createJWT(SECRET_KEY, 3600000L, sampleClaims);

        Assertions.assertThrows(SignatureException.class,
                () -> JwtUtil.parseJWT("wrongSecretKey", token));
    }

    // 边界测试：空claims处理
    @Test
    public void should_handle_empty_claims() {
        Map<String, Object> emptyClaims = new HashMap<>();
        String token = JwtUtil.createJWT(SECRET_KEY, 3600000L, emptyClaims);

        Claims claims = JwtUtil.parseJWT(SECRET_KEY, token);
        Assertions.assertTrue(claims.isEmpty());
    }

}
