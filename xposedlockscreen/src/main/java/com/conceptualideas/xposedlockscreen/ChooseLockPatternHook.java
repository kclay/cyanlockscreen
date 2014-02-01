package com.conceptualideas.xposedlockscreen;

/**
 * Created with IntelliJ IDEA.
 * User: keyston
 * Date: 1/24/14
 * Time: 5:51 PM
 */

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.google.android.collect.Lists;
import de.robv.android.xposed.XposedBridge;

import java.util.Collections;
import java.util.List;

import static de.robv.android.xposed.XposedHelpers.*;

public class ChooseLockPatternHook {


    private static byte mPatternSize;

    public static void init(ClassLoader classLoader) {

        Class<?> clazz = findClass("com.android.settings.ChooseLockPattern$ChooseLockPatternFragment", classLoader);
        findAndHookMethod(clazz, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new MethodHook(true) {
            @Override
            protected void afterHookedMethodX(MethodHookParam param) throws Throwable {


                Fragment view = (Fragment) param.thisObject;

                mPatternSize = view.getActivity().getIntent().getByteExtra("pattern_size",
                        Utils.PATTERN_SIZE_DEFAULT);

                LockPatternCell.updateSize(mPatternSize);

                List<LockPatternView.Cell> mAnimatePattern = Collections.unmodifiableList(Lists.newArrayList(
                        LockPatternCell.of(0, 0, mPatternSize),
                        LockPatternCell.of(0, 1, mPatternSize),
                        LockPatternCell.of(1, 1, mPatternSize),
                        LockPatternCell.of(2, 1, mPatternSize)
                ));

                setObjectField(view, "mAnimatePattern", mAnimatePattern);

                LockPatternView lockPatternView = (LockPatternView) getObjectField(view, "mLockPatternView");
                LockPatternViewHook.hook(lockPatternView).setLockPatternSize(mPatternSize);
                lockPatternView.invalidate();

                super.afterHookedMethodX(param);
            }
        });

        findAndHookMethod(clazz, "saveChosenPatternAndFinish", new MethodHook() {
            @Override
            protected void beforeHookedMethodX(MethodHookParam param) throws Throwable {



                LockPatternUtils utils = new LockPatternUtils(((Fragment) param.thisObject).getActivity());
                // Store lock pattern size to lock_settings db
                LockPatternUtilsHook.hook(utils).setLockPatternSize(mPatternSize);
            }
        });


        clazz = findClass("com.android.settings.ConfirmLockPattern$ConfirmLockPatternFragment", classLoader);

        findAndHookMethod(clazz, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new MethodHook() {
            @Override
            protected void afterHookedMethodX(MethodHookParam param) throws Throwable {
                Fragment view = (Fragment) param.thisObject;
                LockPatternView lockPatternView = (LockPatternView) getObjectField(view, "mLockPatternView");


                LockPatternUtils lockPatternUtils = (LockPatternUtils) getObjectField(view, "mLockPatternUtils");

                LockPatternUtilsHook.hook(lockPatternUtils).configure(lockPatternView);
                //LockPatternViewHook.hook(lockPatternView).setLockPatternSize(.getLockPatternSize());
                //lockPatternView.invalidate();
            }
        });


    }
}
