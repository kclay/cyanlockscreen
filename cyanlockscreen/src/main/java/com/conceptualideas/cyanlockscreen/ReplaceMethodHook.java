package com.conceptualideas.cyanlockscreen;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Created with IntelliJ IDEA.
 * User: keyston
 * Date: 1/24/14
 * Time: 9:56 AM
 */
public abstract class ReplaceMethodHook extends XC_MethodHook {
    private static final boolean DEBUG = Utils.DEBUG;

    public ReplaceMethodHook() {
        super();
    }

    public ReplaceMethodHook(int priority) {
        super(priority);
    }

    @Override
    protected final void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
        try {
            if (DEBUG)
                XposedBridge.log("ReplaceMethodHook.beforeHookMethod -> enter " + param.method.getName());

            Object result = replaceHookedMethod(param);
            param.setResult(result);
            if (DEBUG)
                XposedBridge.log("ReplaceMethodHook.beforeHookMethod -> exit" + param.method.getName() + "Results = " + String.valueOf(param.getResult()));
        } catch (Throwable t) {
            param.setThrowable(t);
            if (DEBUG) {
                XposedBridge.log("ReplaceMethodHook.beforeHookMethod -> error " + param.method.getName());
                XposedBridge.log(t);
            }
        }
    }

    protected final void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    /**
     * Shortcut for replacing a method completely. Whatever is returned/thrown here is taken
     * instead of the result of the original method (which will not be called).
     */
    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;

}
