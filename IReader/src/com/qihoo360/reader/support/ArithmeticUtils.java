/**
 *
 */

package com.qihoo360.reader.support;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 算法工具类
 *
 * @author Jiongxuan Zhang
 */
public class ArithmeticUtils {

    public static byte[] MD5(byte[] input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Utils.error(ArithmeticUtils.class, Utils.getStackTrace(e));
        }
        if (md != null) {
            md.update(input);
            return md.digest();
        } else
            return null;
    }

    public static String getMD5(byte[] input) {
        return ArithmeticUtils.bytesToHexString(MD5(input));
    }

    public static String getMD5(String input) {
        return getMD5(input.getBytes());
    }

    /**
     * 获得一个随机数
     *
     * @param start
     * @param end
     * @return
     */
    public static int getRandom(int start, int end) {
        if (start == end) {
            return start;
        }

        return (int) Math.round(Math.random() * (end - start) + start);
    }

    /**
     * 获得一个随机数
     *
     * @param start
     * @param end
     * @return
     */
    public static long getRandom(long start, long end) {
        if (start == end) {
            return start;
        }

        return Math.round(Math.random() * (end - start) + start);
    }
    /**
     * 
     *  @param bytes
     *  @return    设定文件 
     */
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null)
            return null;
        String table = "0123456789abcdef";
        StringBuilder ret = new StringBuilder(2 * bytes.length);

        for (int i = 0; i < bytes.length; i++) {
            int b;
            b = 0x0f & (bytes[i] >> 4);
            ret.append(table.charAt(b));
            b = 0x0f & bytes[i];
            ret.append(table.charAt(b));
        }

        return ret.toString();
    }

}
