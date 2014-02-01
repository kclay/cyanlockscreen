package com.conceptualideas.cyanlockscreen;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Created with IntelliJ IDEA.
 * User: keyston
 * Date: 1/24/14
 * Time: 9:59 AM
 */
public abstract class MethodHook extends XC_MethodHook {

    private static final boolean DEBUG = Utils.DEBUG;
    private boolean LOG;

    public MethodHook() {
        this(false);
    }

    public MethodHook(boolean log) {
        this.LOG = DEBUG || log;
    }

    @Override
    protected final void beforeHookedMethod(MethodHookParam param) throws Throwable {

        if (LOG)
            XposedBridge.log("MethodHook.beforeHookMethod -> enter " + param.method.getName());
        beforeHookedMethodX(param);
        if (LOG)
            XposedBridge.log("MethodHook.beforeHookMethod -> exit " + param.method.getName());
    }

    @Override
    protected final void afterHookedMethod(MethodHookParam param) throws Throwable {
        if (LOG)
            XposedBridge.log("MethodHook.afterHookMethod -> enter " + param.method.getName());
        afterHookedMethodX(param);
        if (LOG)
            XposedBridge.log("MethodHook.aftereHookMethod -> exit " + param.method.getName());
    }

    protected void beforeHookedMethodX(MethodHookParam param) throws Throwable {
    }

    protected void afterHookedMethodX(MethodHookParam param) throws Throwable {
    }


}
