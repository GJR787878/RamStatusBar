package com.example.ramstatusbar;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RamStatusBar";
    private static final String CLOCK_CLASS = "com.android.systemui.statusbar.policy.Clock";
    private static final String CONFIG_FILE = "/data/local/tmp/ramstatusbar_mode";

    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    private static final int[] COMMON_RAM_TIERS_GB = {3, 4, 6, 8, 12, 16, 18, 24, 32};

    private static final long UPDATE_INTERVAL_MS = 1000;

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
                    int mode = readModeFromFile();

                    String time = timeFormat.format(new Date());
                    String ram = getRamInfo(context);
                    String full = time + " " + ram;
                    int targetLen = full.length();

                    String display;
                    switch (mode) {
                        case MODE_TIME_ONLY:
                            display = padToLength(time, targetLen);
                            break;
                        case MODE_RAM_ONLY:
                            display = padToLength(ram, targetLen);
                            break;
                        case MODE_TIME_RAM:
                        default:
                            display = full;
                            break;
                    }

                    clockView.setText(display);
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": 更新文字出错: " + t);
                }
                getHandler().postDelayed(this, UPDATE_INTERVAL_MS);
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

    private int readModeFromFile() {
        try {
            File f = new File(CONFIG_FILE);
            if (!f.exists()) {
                return MODE_TIME_RAM;
            }
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = br.readLine();
            br.close();
            if (line == null) {
                return MODE_TIME_RAM;
            }
            return Integer.parseInt(line.trim());
        } catch (Throwable t) {
            return MODE_TIME_RAM;
        }
    }

    private String padToLength(String content, int targetLength) {
        int diff = targetLength - content.length();
        if (diff <= 0) {
            return content;
        }
        int left = diff / 2;
        int right = diff - left;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < left; i++) {
            sb.append(' ');
        }
        sb.append(content);
        for (int i = 0; i < right; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private String getRamInfo(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(info);

        double availGb = info.availMem / 1024.0 / 1024.0 / 1024.0;
        double rawTotalGb = info.totalMem / 1024.0 / 1024.0 / 1024.0;
        int totalGb = roundToCommonTier(rawTotalGb);

        return String.format(Locale.getDefault(), "%.1fG/%dG", availGb, totalGb);
    }

    private int roundToCommonTier(double rawTotalGb) {
        for (int tier : COMMON_RAM_TIERS_GB) {
            if (rawTotalGb <= tier + 0.5) {
                return tier;
            }
        }
        return (int) Math.round(rawTotalGb);
    }
}
