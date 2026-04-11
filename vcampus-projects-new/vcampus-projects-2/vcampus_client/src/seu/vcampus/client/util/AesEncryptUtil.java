package seu.vcampus.client.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * AES加密工具类（可逆，用于客户端密码加密存储）
 * 模式：AES/CBC/PKCS5Padding（CBC模式需IV，更安全）
 */
public class AesEncryptUtil {
    // 固定算法和模式（不要修改，否则解密会失败）
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";

    // 加密：明文密码 → Base64编码的密文（包含IV，格式：IV:密文）
    public static String encrypt(String plainText, String aesKey) throws Exception {
        // 1. 生成16位随机IV（初始化向量）
        byte[] iv = new byte[16];
        new java.security.SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // 2. 初始化AES密钥
        SecretKeySpec keySpec = new SecretKeySpec(aesKey.getBytes(), KEY_ALGORITHM);

        // 3. 加密
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

        // 4. 拼接IV和密文（Base64编码，便于存储）
        String ivBase64 = Base64.getEncoder().encodeToString(iv);
        String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes);
        return ivBase64 + ":" + encryptedBase64; // 格式：IV:密文
    }

    // 解密：Base64编码的密文（IV:密文）→ 明文密码
    public static String decrypt(String encryptedText, String aesKey) throws Exception {
        // 1. 拆分IV和密文
        String[] parts = encryptedText.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("加密格式错误，需包含IV和密文");
        }
        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] encryptedBytes = Base64.getDecoder().decode(parts[1]);

        // 2. 初始化IV和密钥
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKeySpec keySpec = new SecretKeySpec(aesKey.getBytes(), KEY_ALGORITHM);

        // 3. 解密
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, "UTF-8");
    }
}