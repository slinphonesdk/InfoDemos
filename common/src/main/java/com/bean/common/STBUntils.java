package com.bean.common;


public class STBUntils
{
    public final static byte[] stringToByte(final String s) {
        String str = s.substring(1,s.length()-1); // 去[]
        String[] strs = str.split(",");
        byte[] bytes = new byte[strs.length];
        for (int i = 0; i < strs.length; i++) {
            bytes[i] = Byte.valueOf(strs[i]);
        }
        return bytes;
    }
}
