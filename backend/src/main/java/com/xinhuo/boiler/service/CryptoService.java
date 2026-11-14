package com.xinhuo.boiler.service;

import com.xinhuo.boiler.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {
    private static final String PREFIX = "enc:";
    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(@Value("${app.encrypt-key}") String key) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        this.keySpec = new SecretKeySpec(digest, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new BizException("敏感数据加密失败");
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (!value.startsWith(PREFIX)) {
            return value;
        }
        try {
            byte[] data = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            byte[] iv = new byte[12];
            System.arraycopy(data, 0, iv, 0, iv.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(128, iv));
            byte[] plain = cipher.doFinal(data, iv.length, data.length - iv.length);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String cleaned = phone.replaceAll("\\s", "");
        if (cleaned.length() >= 7) {
            return cleaned.substring(0, 3) + "****" + cleaned.substring(cleaned.length() - 4);
        }
        return "****";
    }

    public String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String plain = decrypt(value);
        if (plain.length() <= 8) {
            return "********";
        }
        return plain.substring(0, 4) + "********" + plain.substring(plain.length() - 4);
    }
}
