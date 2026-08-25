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
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RamStatusBar";
    private static final String[] TARGET_CLASSES = new String[] {
            "com.android.systemui.shared.clocks.view.FlexClockTextView",
            "android.widget.TextClock",
            "com.android.systemui.statusbar.policy.Clock",
    };

    private Handler mHandler;
    private final Map<TextView, Runnable> mUpdaters = new HashMap<>();

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(
                    TextView.class,
                    "onAttachedToWindow",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                TextView tv = (TextView) param.thisObject;
                                String cls = tv.getClass().getName();
                                if (isTargetClass(cls)) {
                                    XposedBridge.log(TAG + ": 命中目标类 -> " + cls);
                                    startUpdating(tv);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + ": attach 处理出错: " + t);
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(
                    android.view.View.class,
                    "onDetachedFromWindow",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                if (param.thisObject instanceof TextView) {
                                    stopUpdating((TextView) param.thisObject);
                                }
                            } catch (Throwable t) {
                            }
                        }
                    });

            XposedBridge.log(TAG + ": hook 安装成功，白名单共 " + TARGET_CLASSES.length + " 个候选类");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook 安装失败: " + t);
        }
    }

    private boolean isTargetClass(String cls) {
        for (String candidate : TARGET_CLASSES) {
            if (candidate.equals(cls)) {
                return true;
            }
        }
        return false;
    }

    private void startUpdating(final TextView clockView) {
        stopUpdating(clockView);

        final Context context = clockView.getContext();
        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        Runnable updater = new Runnable() {
            @Override
            public void run() {
                try {
                    String time = timeFormat.format(new Date());
                    String ram = getRamInfo(context);
                    clockView.setText(time + "  " + ram);
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": 更新文字出错: " + t);
                }
                getHandler().postDelayed(this, 5000);
            }
        };
        mUpdaters.put(clockView, updater);
        XposedBridge.log(TAG + ": 已接管一个时钟实例");
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
        double totalGb = info.totalMem / 1024.0 / 1024.0 / 1024.0;

        return String.format(Locale.getDefault(), "%.1fG/%.0fG", availGb, totalGb);
    }
}
