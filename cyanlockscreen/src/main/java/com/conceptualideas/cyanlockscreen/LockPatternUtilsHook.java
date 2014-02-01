package com.conceptualideas.cyanlockscreen;


import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.widget.ILockSettings;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.google.android.collect.Lists;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static de.robv.android.xposed.XposedHelpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: keyston
 * Date: 1/20/14
 * Time: 7:31 PM
 */
public class LockPatternUtilsHook {
    private static final String TAG = "LockPatternUtilsHook";
    /**
     * Determines the width and height of the LockPatternView widget
     *
     * @hide
     */
    public static final String LOCK_PATTERN_SIZE = "lock_pattern_size";
    /**
     * Whether lock pattern will show dots (0 = false, 1 = true)
     *
     * @hide
     */
    public static final String LOCK_DOTS_VISIBLE = "lock_pattern_dotsvisible";

    /**
     * Whether lockscreen error pattern is visible (0 = false, 1 = true)
     *
     * @hide
     */
    public static final String LOCK_SHOW_ERROR_PATH = "lock_pattern_show_error_path";

    /**
     * Whether the unsecure widget screen will be shown before a secure
     * lock screen
     *
     * @hide
     */
    public static final String LOCK_BEFORE_UNLOCK =
            "lock_before_unlock";

    /**
     * The default size of the pattern lockscreen. Ex: 3x3
     */
    public static final byte PATTERN_SIZE_DEFAULT = 3;
    public final static String LOCKSCREEN_CAMERA_ENABLED = "lockscreen.camera_enabled";
    private ILockSettings mLockSettingsService;

    private LockPatternUtils underlying;
    private static LockPatternUtilsHook sInstance;


    public static LockPatternUtilsHook hook(LockPatternUtils patternUtils) {
        if (sInstance == null) {
            sInstance = new LockPatternUtilsHook();
        }
        if (patternUtils != null)
            sInstance.setUtils(patternUtils);
        return sInstance;
    }

    public static LockPatternUtilsHook get() {
        return sInstance;
    }

    private LockPatternUtils utils() {
        return underlying;
    }

    private static LockPatternUtilsHook hook(XC_MethodHook.MethodHookParam param) {
        return hook((LockPatternUtils) (param.thisObject));
    }

    public static void init() {
        XposedBridge.hookAllConstructors(LockPatternUtils.class, new MethodHook() {
            @Override
            protected void afterHookedMethodX(MethodHookParam param) throws Throwable {
                hook(param);

            }
        });
        findAndHookMethod(LockPatternUtils.class, "stringToPattern", String.class, new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return hook(methodHookParam).stringToPattern((String) methodHookParam.args[0]);
            }
        });

        List<LockPatternView.Cell> c = Lists.newArrayList();
        Method m = findMethodBestMatch(LockPatternUtils.class, "patternToString", c);

        XposedBridge.hookMethod(m, new ReplaceMethodHook() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return hook(methodHookParam).patternToString((List<LockPatternView.Cell>) methodHookParam.args[0]);
            }
        });


    }


    public List<LockPatternView.Cell> stringToPattern(String string) {
        List<LockPatternView.Cell> result = Lists.newArrayList();

        final byte size = getLockPatternSize();

        LockPatternCell.updateSize(size);

        final byte[] bytes = string.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            result.add(LockPatternCell.of(b / size, b % size, size));
        }
        return result;
    }

    /**
     * Serialize a pattern.
     *
     * @param pattern The pattern.
     * @return The pattern in string form.
     */
    public String patternToString(List<LockPatternView.Cell> pattern) {
        if (pattern == null) {
            return "";
        }
        final byte lockPatternSize = getLockPatternSize();
        final int patternSize = pattern.size();

        byte[] res = new byte[patternSize];
        for (int i = 0; i < patternSize; i++) {
            LockPatternView.Cell cell = pattern.get(i);
            res[i] = (byte) (cell.getRow() * lockPatternSize + cell.getColumn());
        }
        return new String(res);
    }

    /*
     * Generate an SHA-1 hash for the pattern. Not the most secure, but it is
     * at least a second level of protection. First level is that the file
     * is in a location only readable by the system process.
     * @param pattern the gesture pattern.
     * @return the hash of the pattern in a byte array.
     */
    public byte[] patternToHash(List<LockPatternView.Cell> pattern) {
        if (pattern == null) {
            return null;
        }

        final byte lockPatternSize = getLockPatternSize();
        final int patternSize = pattern.size();
        byte[] res = new byte[patternSize];
        for (int i = 0; i < patternSize; i++) {
            LockPatternView.Cell cell = pattern.get(i);
            res[i] = (byte) (cell.getRow() * lockPatternSize + cell.getColumn());
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(res);
            return hash;
        } catch (NoSuchAlgorithmException nsa) {
            return res;
        }
    }

    private ILockSettings getLockSettings() {
        if (mLockSettingsService == null) {
            mLockSettingsService = ILockSettings.Stub.asInterface(
                    (IBinder) ServiceManager.getService("lock_settings"));
        }
        return mLockSettingsService;
    }

    private long getLong(String secureSettingKey, long defaultValue) {
        try {
            return getLockSettings().getLong(secureSettingKey, defaultValue,
                    getCurrentOrCallingUserId());
        } catch (RemoteException re) {
            return defaultValue;
        }
    }

    /**
     * @return the pattern lockscreen size
     */
    public byte getLockPatternSize() {

        return (byte) getLong(LOCK_PATTERN_SIZE, Utils.PATTERN_SIZE_DEFAULT);

    }

    private boolean getBoolean(String secureSettingKey, boolean defaultValue, int userId) {
        try {
            return getLockSettings().getBoolean(secureSettingKey, defaultValue, userId);
        } catch (RemoteException re) {
            return defaultValue;
        }
    }

    private boolean getBoolean(String secureSettingKey, boolean defaultValue) {
        return getBoolean(secureSettingKey, defaultValue, getCurrentOrCallingUserId());
    }

    private void setString(String secureSettingKey, String value, int userHandle) {
        try {
            getLockSettings().setString(secureSettingKey, value, userHandle);
        } catch (RemoteException re) {
            // What can we do?
            Log.e(TAG, "Couldn't write string " + secureSettingKey + re);
        }
    }

    private void setLong(String secureSettingKey, long value) {
        setLong(secureSettingKey, value, getCurrentOrCallingUserId());
    }

    private void setLong(String secureSettingKey, long value, int userHandle) {
        try {
            getLockSettings().setLong(secureSettingKey, value, getCurrentOrCallingUserId());
        } catch (RemoteException re) {
            // What can we do?
            Log.e(TAG, "Couldn't write long " + secureSettingKey + re);
        }
    }


    /**
     * Set the pattern lockscreen size
     */
    public void setLockPatternSize(long size) {
        setLong(LOCK_PATTERN_SIZE, size);
    }


    public void setVisibleDotsEnabled(boolean enabled) {
        setBoolean(LOCK_DOTS_VISIBLE, enabled);
    }


    public boolean isVisibleDotsEnabled() {
        return getBoolean(LOCK_DOTS_VISIBLE, true);
    }

    public void setShowErrorPath(boolean enabled) {
        setBoolean(LOCK_SHOW_ERROR_PATH, enabled);
    }

    public boolean isShowErrorPath() {
        return getBoolean(LOCK_SHOW_ERROR_PATH, true);
    }

    private int getCurrentOrCallingUserId() {
        return (Integer) callMethod(utils(), "getCurrentOrCallingUserId");

    }

    private void setBoolean(String secureSettingKey, boolean enabled) {
        setBoolean(secureSettingKey, enabled, getCurrentOrCallingUserId());
    }

    private void setBoolean(String secureSettingKey, boolean enabled, int userId) {
        try {
            getLockSettings().setBoolean(secureSettingKey, enabled, userId);
        } catch (RemoteException re) {
            // What can we do?
            Log.e(TAG, "Couldn't write boolean " + secureSettingKey + re);
        }
    }

    public boolean getCameraEnabled() {
        return getCameraEnabled(getCurrentOrCallingUserId());
    }

    public boolean getCameraEnabled(int userId) {
        return getBoolean(LOCKSCREEN_CAMERA_ENABLED, true, userId);
    }

    public void setCameraEnabled(boolean enabled) {
        setCameraEnabled(enabled, getCurrentOrCallingUserId());
    }

    public void setCameraEnabled(boolean enabled, int userId) {
        setBoolean(LOCKSCREEN_CAMERA_ENABLED, enabled, userId);
    }

    /**
     * @hide Set the lock-before-unlock option (show widgets before the secure
     * unlock screen). See config_enableLockBeforeUnlockScreen
     */
    public void setLockBeforeUnlock(boolean enabled) {
        setBoolean(LOCK_BEFORE_UNLOCK, enabled);
    }

    public void setUtils(LockPatternUtils utils) {
        //   XposedBridge.log("setUtils");
        this.underlying = utils;//new WeakReference<LockPatternUtils>(utils);g
/*
        Context context = (Context) getObjectField(utils,"mContext");
        if(context instanceof Activity){
            Activity a = ((Activity)context);
            XposedBridge.log("TRACK=Activity = "+a.getPackageName()+" :: "+a.getPackageCodePath());
        }else{
            XposedBridge.log("TRACK=Context = "+context.getPackageName() + " :: "+context.getPackageCodePath());
        }*/
    }

    public void configure(final LockPatternView lockPatternView) {

        final LockPatternViewHook viewHook = LockPatternViewHook.hook(lockPatternView);
        viewHook.setLockPatternSize(getLockPatternSize());
        viewHook.setVisibleDots(isVisibleDotsEnabled());

        viewHook.setShowErrorPath(isShowErrorPath());
        lockPatternView.invalidate();

    }
}
