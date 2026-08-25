package com.example.ramstatusbar;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RamStatusBar";
    private static final String CLOCK_CLASS = "com.android.systemui.statusbar.policy.Clock";

    private Handler mHandler;
    private Runnable mUpdater;

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
                    CLOCK_CLASS,
                    lpparam.classLoader,
                    "onAttachedToWindow",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            TextView clockView = (TextView) param.thisObject;
                            startUpdating(clockView);
                        }
                    });

            XposedHelpers.findAndHookMethod(
                    CLOCK_CLASS,
                    lpparam.classLoader,
                    "onDetachedFromWindow",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            stopUpdating();
                        }
                    });

            XposedBridge.log(TAG + ": hook 安装成功 -> " + CLOCK_CLASS);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook 失败，可能这个 ROM 的时钟类名不一样: " + t);
        }
    }

    private void startUpdating(final TextView clockView) {
        stopUpdating();

        final Context context = clockView.getContext();
        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        mUpdater = new Runnable() {
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
        getHandler().post(mUpdater);
    }

    private void stopUpdating() {
        if (mUpdater != null && mHandler != null) {
            mHandler.removeCallbacks(mUpdater);
            mUpdater = null;
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
