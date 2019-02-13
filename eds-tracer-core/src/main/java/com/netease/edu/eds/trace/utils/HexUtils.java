package com.netease.edu.eds.trace.utils;

import java.nio.charset.Charset;

public class HexUtils {


    public static final String bytesStrToHexStr(String str) {
        byte[] bcd = str.getBytes(Charset.forName("utf-8"));
        return bytesToHexStr(bcd);
    }

    public static final String hexStringToBytesStr(String str) {
        byte[] bcd = hexStrToBytes(str);
        return new String(bcd, Charset.forName("utf-8"));
    }

    /**
     * 将字节数组转换为16进制字符串的形式.
     */
    public static final String bytesToHexStr(byte[] bcd) {
        StringBuffer s = new StringBuffer(bcd.length * 2);

        for (int i = 0; i < bcd.length; i++) {
            s.append(bcdLookup[(bcd[i] >>> 4) & 0x0f]);
            s.append(bcdLookup[bcd[i] & 0x0f]);
        }

        return s.toString();
    }

    /**
     * 将16进制字符串还原为字节数组.
     */
    public static final byte[] hexStrToBytes(String s) {
        byte[] bytes;

        bytes = new byte[s.length() / 2];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }

        return bytes;
    }

    private static final char[] bcdLookup = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
            'e', 'f'                     };

}
