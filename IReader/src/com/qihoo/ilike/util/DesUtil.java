package com.qihoo.ilike.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DesUtil {
    /**
     * 原文：1234567890 加密后：DAPuqXUOOYyueVvpS+NSfA==
     */

    private static final String ALGORITHM_DES = "DES/CBC/PKCS5Padding";
    private static final String KEY = "q1h0036o";

    public static String encryptDES(String encryptStr) {
        String ret = "";
        IvParameterSpec zeroIv = null;
        SecretKeySpec key = null;
        Cipher cipher = null;
        byte[] encryptedData = null;
        try {
            if (encryptStr != null && !encryptStr.equals("")) {
                zeroIv = new IvParameterSpec(KEY.getBytes());
                key = new SecretKeySpec(KEY.getBytes(), "DES");
                cipher = Cipher.getInstance(ALGORITHM_DES);
                cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
                encryptedData = cipher.doFinal(encryptStr.getBytes());
                ret = Base64.encodeToString(encryptedData, Base64.DEFAULT);
            }
        } catch (Exception e) {
            com.qihoo360.reader.support.Utils.error(DesUtil.class, e
                    .getStackTrace().toString());
            ret = "";
        }
        return ret;
    }

    public static String decryptDES(String decryptStr) {
        String ret = "";
        byte[] byteMi = null, decryptData = null;
        IvParameterSpec zeroIv = null;
        SecretKeySpec key = null;
        Cipher cipher = null;
        try {
            if (decryptStr != null && !decryptStr.equals("")) {
                byteMi = Base64.decode(decryptStr, Base64.DEFAULT);
                zeroIv = new IvParameterSpec(KEY.getBytes());
                key = new SecretKeySpec(KEY.getBytes(), "DES");
                cipher = Cipher.getInstance(ALGORITHM_DES);
                cipher.init(Cipher.DECRYPT_MODE, key, zeroIv);
                decryptData = cipher.doFinal(byteMi);
                ret = new String(decryptData);
            }
        } catch (Exception e) {
            com.qihoo360.reader.support.Utils.error(DesUtil.class, e
                    .getStackTrace().toString());
            ret = "";
        }
        return ret;
    }

}