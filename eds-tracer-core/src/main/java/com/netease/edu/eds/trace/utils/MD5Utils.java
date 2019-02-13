package com.netease.edu.eds.trace.utils;

import java.security.MessageDigest;

public class MD5Utils {

    public static String digest(String rawString) {
        try {
            return compute(rawString);
        } catch (Exception e) {
            return "";
        }
    }

    public static String digest(byte[] rawBytes) {
        try {
            return compute(rawBytes);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Computes the MD5 fingerprint of a string.
     * 
     * @return the MD5 digest of the input <code>String</code>
     */
    private static String compute(String inStr) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] md5Bytes = md5.digest(inStr.getBytes("UTF-8"));
        return toHexString(md5Bytes);
    }

    private static String compute(byte[] inBytes) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] md5Bytes = md5.digest(inBytes);
        return toHexString(md5Bytes);
    }

    private static String toHexString(byte[] bytes) {
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int val = ((int) bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

}
