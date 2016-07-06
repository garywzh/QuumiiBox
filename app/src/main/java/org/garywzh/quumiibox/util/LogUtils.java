package org.garywzh.quumiibox.util;

import android.util.Log;

import org.garywzh.quumiibox.BuildConfig;


public class LogUtils {
    public static void v(String tag, String msg) {
        if (BuildConfig.DEBUG) Log.v(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (BuildConfig.DEBUG) Log.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (BuildConfig.DEBUG) Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void wtf(String tag, String msg) {
        Log.wtf(tag, msg);
    }
}
