package com.conceptualideas.xposedlockscreen;

import android.os.Build;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: keyston
 * Date: 1/25/14
 * Time: 11:33 AM
 */
public class KeyguardPatternViewHook {
    ;

    public static void init(ClassLoader classLoader) {

        String packageName;
        final int sdk = Build.VERSION.SDK_INT;
         if(sdk == Build.VERSION_CODES.JELLY_BEAN){
             packageName = "com.android.internal.policy.";
         }else if(sdk <Build.VERSION_CODES.KITKAT){
             packageName="com.android.internal.policy.impl.";
         }else{
             packageName="com.android.";
         }

        Class c = findClass(packageName+"keyguard.KeyguardPatternView", classLoader);


        findAndHookMethod(c, "onFinishInflate", new MethodHook(true) {
            @Override
            protected void afterHookedMethodX(MethodHookParam param) throws Throwable {
                Object o = param.thisObject;
                LockPatternView mLockPatternView = (LockPatternView) getObjectField(o, "mLockPatternView");


                LockPatternUtils utils = (LockPatternUtils) getObjectField(o, "mLockPatternUtils");
                LockPatternUtilsHook utilsHook = LockPatternUtilsHook.hook(utils);

                utilsHook.configure(mLockPatternView);

            }
        });

    }
}
