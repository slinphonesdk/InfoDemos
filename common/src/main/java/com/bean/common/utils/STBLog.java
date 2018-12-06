package com.bean.common.utils;

import android.util.Log;

public class STBLog
{
    public static Boolean isDebug = false;
    public static void out(String tag, String msg) {
        if (isDebug) {
            Log.e(tag, msg);
        }
    }
}
