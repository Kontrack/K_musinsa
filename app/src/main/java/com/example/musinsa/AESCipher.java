package com.example.musinsa;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESCipher {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String CHARSET = "UTF-8";
    private final byte[] key;

    public AESCipher(String key) throws Exception {
        this.key = key.getBytes(CHARSET);
    }

    public String encrypt(String message) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]); // 16바이트의 초기화 벡터
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(message.getBytes(CHARSET));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String encryptedMessage) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(new byte[16]); // 16바이트의 초기화 벡터
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage);
        byte[] decrypted = cipher.doFinal(decodedBytes);
        return new String(decrypted, CHARSET);
    }
}