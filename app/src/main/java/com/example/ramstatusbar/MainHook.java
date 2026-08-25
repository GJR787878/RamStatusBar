package com.example.ramstatusbar;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RamStatusBar";
    private static final String CLOCK_CLASS = "com.android.systemui.statusbar.policy.Clock";
    private static final String MODULE_PACKAGE = "com.example.ramstatusbar";
    private static final String PREFS_NAME = "config";
    private static final String KEY_SHOW_TIME = "show_time";

    private static final int DISPLAY_TOTAL_GB = 8;

    private Handler mHandler;
    private final Map<TextView, Runnable> mUpdaters = new HashMap<>();
    private XSharedPreferences mPrefs;

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    private XSharedPreferences getPrefs() {
        if (mPrefs == null) {
            mPrefs = new XSharedPreferences(MODULE_PACKAGE, PREFS_NAME);
            mPrefs.makeWorldReadable();
        }
        return mPrefs;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        try {
            Class<?> clockClass = Class.forName(CLOCK_CLASS, false, lpparam.classLoader);

            XposedBridge.hookAllConstructors(clockClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        TextView tv = (TextView) param.thisObject;
                        startUpdating(tv);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + ": 构造后处理出错: " + t);
                    }
                }
            });

            XposedBridge.log(TAG + ": 构造函数hook 安装成功 -> " + CLOCK_CLASS);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook 安装失败: " + t);
        }
    }

    private void startUpdating(final TextView clockView) {
        stopUpdating(clockView);

        final Context context = clockView.getContext();
        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        Runnable updater = new Runnable() {
            @Override
            public void run() {
                try {
                    getPrefs().reload();
                    boolean showTime = getPrefs().getBoolean(KEY_SHOW_TIME, true);

                    String ram = getRamInfo(context);

                    if (showTime) {
                        String time = timeFormat.format(new Date());
                        String full = time + " " + ram;

                        android.text.SpannableString spannable =
                                new android.text.SpannableString(full);
                        int ramStart = time.length() + 1;
                        spannable.setSpan(
                                new android.text.style.RelativeSizeSpan(0.65f),
                                ramStart,
                                full.length(),
                                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        clockView.setText(spannable);
                    } else {
                        clockView.setText(ram);
                    }
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": 更新文字出错: " + t);
                }
                getHandler().postDelayed(this, 5000);
            }
        };
        mUpdaters.put(clockView, updater);
        getHandler().post(updater);
    }

    private void stopUpdating(TextView clockView) {
        Runnable updater = mUpdaters.remove(clockView);
        if (updater != null && mHandler != null) {
            mHandler.removeCallbacks(updater);
        }
    }

    private String getRamInfo(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(info);

        double availGb = info.availMem / 1024.0 / 1024.0 / 1024.0;

        return String.format(Locale.getDefault(), "%.1fG/%dG", availGb, DISPLAY_TOTAL_GB);
    }
}
