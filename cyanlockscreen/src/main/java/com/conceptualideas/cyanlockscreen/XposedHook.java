package com.conceptualideas.cyanlockscreen;

import android.content.res.XModuleResources;
import com.android.internal.widget.LockPatternView;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


import java.util.List;

import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: keyston
 * Date: 1/18/14
 * Time: 5:09 PM
 */
public class XposedHook implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {

    private static String MODULE_PATH = null;
    private static final String PACKAGE_NAME = "com.android.settings";
    private static final String CLASS_NAME = PACKAGE_NAME + ".ChooseLockPattern";

    private static final String FRAGMENT_NAME = CLASS_NAME + ".$ChooseLockPatternFragment";

    private List<LockPatternView.Cell> mAnimatePattern;

    int mPatternSize;

    // mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        final String packageName = loadPackageParam.packageName;
        final boolean isSettings = packageName.equals(PACKAGE_NAME);
        final boolean isSystemUI = packageName.equals("com.android.systemui");
        final boolean isKeyguard = packageName.equals("com.android.keyguard");
        final boolean isAndroid = packageName.equals("android");
       // final boolean isPolicy = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ? packageName.contains("policy") : false;
        final boolean isServer = packageName.equals("com.android.server");
        final boolean isAcceptable = isSettings || isSettings || isServer || isKeyguard || isSystemUI || isAndroid ;


        if (!isAcceptable) return;


        try {
            LockPatternCell.init();
        } catch (Throwable t) {
            XposedBridge.log(t);

        }
        try {
            LockPatternUtilsHook.init();
        } catch (Throwable t) {
            XposedBridge.log(t);

        }
        XposedBridge.log("Accept = " + loadPackageParam.packageName + " :: " + loadPackageParam.processName);


        if (isSettings) {
            ChooseLockPatternHook.init(loadPackageParam.classLoader);
            ChooseLockGenericHook.init(loadPackageParam.classLoader);

        }


        if (isKeyguard || isAndroid)
            try {
                KeyguardPatternViewHook.init(loadPackageParam.classLoader);
            } catch (Throwable t) {
                XposedBridge.log(t);

            }


        try {
            LockPatternViewHook.init();
        } catch (Throwable t) {
            XposedBridge.log(t);
        }


    }

    private static void tryFindClass(String className, XC_LoadPackage.LoadPackageParam loadPackageParam) {

        try {
            findClass(className, loadPackageParam.classLoader);
            XposedBridge.log(" Found " + className + "  = " + loadPackageParam.packageName + " :: " + loadPackageParam.processName);
        } catch (Throwable t) {

        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resourcesParam) throws Throwable {

        if (!resourcesParam.packageName.equals(PACKAGE_NAME)) return;

        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resourcesParam.res);

        resourcesParam.res.setReplacement(PACKAGE_NAME, "xml", "security_settings_picker", modRes.fwd(R.xml.security_settings_picker));
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }
}
