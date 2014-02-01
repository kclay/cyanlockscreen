package com.conceptualideas.xposedlockscreen;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

import java.util.concurrent.Callable;

import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: keyston
 * Date: 1/26/14
 * Time: 8:26 AM
 */
public class ChooseLockGenericHook {


    private static byte LOCK_PATTERN_SIZE = 0;
    private static final String KEY_UNLOCK_SET_PATTERN = "unlock_set_pattern";
    private static final String KEY_UNLOCK_SET_PATTERN_4x4 = "unlock_set_pattern_4x4";
    private static final String KEY_UNLOCK_SET_PATTERN_5x5 = "unlock_set_pattern_5x5";
    private static final String KEY_UNLOCK_SET_PATTERN_6x6 = "unlock_set_pattern_6x6";

    public static void init(ClassLoader classLoader) {


        Class<?> clazz = findClass("com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment", classLoader);

        findAndHookMethod(clazz, "onPreferenceTreeClick", PreferenceScreen.class, Preference.class, new MethodHook() {
            @Override
            protected void beforeHookedMethodX(MethodHookParam param) throws Throwable {

                Preference preference = (Preference) param.args[1];
                final String key = preference.getKey();

                if (KEY_UNLOCK_SET_PATTERN.equals(key)) {

                    LOCK_PATTERN_SIZE = Utils.PATTERN_SIZE_DEFAULT; // default
                } else if (KEY_UNLOCK_SET_PATTERN_4x4.equals(key)) {
                    LOCK_PATTERN_SIZE = 4;
                } else if (KEY_UNLOCK_SET_PATTERN_5x5.equals(key)) {
                    LOCK_PATTERN_SIZE = 5;
                } else if (KEY_UNLOCK_SET_PATTERN_6x6.equals(key)) {
                    LOCK_PATTERN_SIZE = 6;
                }

                if (LOCK_PATTERN_SIZE > 0) {

                    // reset back so that default logic is called
                    callMethod(param.thisObject, "updateUnlockMethodAndFinish",
                            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING, false);
                    param.setResult(true);
                    // Set back to 0 so that settings menu works as before
                    LOCK_PATTERN_SIZE = 0;

                }
                super.beforeHookedMethodX(param);
            }
        });

        MethodHook hook = new MethodHook() {
            @Override
            protected void beforeHookedMethodX(MethodHookParam param) throws Throwable {

                if (LOCK_PATTERN_SIZE == 0) return;
                Intent intent = (Intent) param.args[0];
                if (!intent.hasExtra("key_lock_method") || !intent.getStringExtra("key_lock_method").equals("pattern"))
                    return;

                intent.putExtra("pattern_size", LOCK_PATTERN_SIZE);


                super.beforeHookedMethodX(param);
            }
        };


        XposedBridge.hookMethod(findMethodBestMatch(clazz, "startActivityForResult", Intent.class, int.class), hook);
        XposedBridge.hookMethod(findMethodBestMatch(clazz, "startActivity", Intent.class), hook);


    }


}
