package com.conceptualideas.xposedlockscreen;


import android.content.Intent;
import android.os.Bundle;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

/**
 * Created with IntelliJ IDEA.
 * User: keyston
 * Date: 1/25/14
 * Time: 9:49 AM
 */
public class Utils {
    public static final boolean DEBUG = false;

    public static final boolean OUTPUT_CALLER = true;

    public static final byte PATTERN_SIZE_DEFAULT = 3;

    public static final XSharedPreferences prefs = new XSharedPreferences(Utils.class.getPackage().getName());

    public static void caller() {
        if (!OUTPUT_CALLER) return;

        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StackTraceElement element = elements[5];
        XposedBridge.log(String.format("Caller = %s at %s", element.getMethodName(), element.getClassName()));
    }

    public static void dump(Intent intent) {
        Bundle b = intent.getExtras();
        XposedBridge.log("START DUMP");
        for (String key : b.keySet()) {
            XposedBridge.log(String.format("[ %s = %s ]", key, b.get(key)));
        }
        XposedBridge.log("END DUMP");
    }
}
