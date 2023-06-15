package org.ie;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;

public class Crypto {

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String MAC_ALGORITHM = "HmacSHA256";
    private static final int MAC_LENGTH = 32; // Length of the MAC in bytes

    public static byte[] intToBytes(int value) {
        return new byte[] {
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }
     
    public static long stringToLong(String numberString) {
        return Long.parseLong(numberString);
    }

    public static int stringToInt(String numberString) {
        return Integer.parseInt(numberString);
    }

    public static String longToString(long number) {
        return String.valueOf(number);
    }

    public static String intToString(int number) {
        return String.valueOf(number);
    }

    public static byte[] stringToBytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public static String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256); // Set the desired key size (256 bits for AES-256)
        return keyGenerator.generateKey();
    }

    public static byte[] encrypt(byte[] plaintext, SecretKey secretKey) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(plaintext);
    }

    public static byte[] decrypt(byte[] ciphertext, SecretKey secretKey) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(ciphertext);
    }

    public static String encryptAndEncode(String value, SecretKey secretKey) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException {
        byte[] plaintext;
       
        plaintext = stringToBytes((String) value);
       

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext);

        Mac mac = Mac.getInstance(MAC_ALGORITHM);
        mac.init(secretKey);
        byte[] macBytes = mac.doFinal(encryptedBytes);

        byte[] finalBytes = new byte[encryptedBytes.length + macBytes.length];
        System.arraycopy(encryptedBytes, 0, finalBytes, 0, encryptedBytes.length);
        System.arraycopy(macBytes, 0, finalBytes, encryptedBytes.length, macBytes.length);

        return Base64.getEncoder().encodeToString(finalBytes);
    }

    public static String decodeAndDecrypt(String encryptedString, SecretKey secretKey) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        byte[] finalBytes = Base64.getDecoder().decode(encryptedString);

        int encryptedBytesLength = finalBytes.length - MAC_LENGTH;
        byte[] encryptedBytes = new byte[encryptedBytesLength];
        byte[] receivedMac = new byte[MAC_LENGTH];

        System.arraycopy(finalBytes, 0, encryptedBytes, 0, encryptedBytesLength);
        System.arraycopy(finalBytes, encryptedBytesLength, receivedMac, 0, MAC_LENGTH);

        Mac mac = Mac.getInstance(MAC_ALGORITHM);
        mac.init(secretKey);
        byte[] calculatedMac = mac.doFinal(encryptedBytes);

        if (!MessageDigest.isEqual(receivedMac, calculatedMac)) {
            throw new SecurityException("MAC verification failed. The encrypted data may have been tampered with.");
        }

        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

      
        return bytesToString(decryptedBytes);
        
    }

}
