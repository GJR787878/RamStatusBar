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
import de.robv.android.xposed.XposedHelpers;
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
    private final Map<TextView, SimpleDateFormat> mManaged = new HashMap<>();
    private boolean mApplyingOurText = false;

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
                        startManaging(tv);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + ": 构造后处理出错: " + t);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(
                    TextView.class,
                    "setText",
                    CharSequence.class,
                    TextView.BufferType.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                if (mApplyingOurText) {
                                    return;
                                }
                                TextView tv = (TextView) param.thisObject;
                                if (mManaged.containsKey(tv)) {
                                    applyDisplayNow(tv);
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });

            XposedBridge.log(TAG + ": hook 安装成功 -> " + CLOCK_CLASS);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook 安装失败: " + t);
        }
    }

    private void startManaging(final TextView clockView) {
        mManaged.put(clockView, new SimpleDateFormat("HH:mm", Locale.getDefault()));
        applyDisplayNow(clockView);

        Runnable poller = new Runnable() {
            @Override
            public void run() {
                if (mManaged.containsKey(clockView)) {
                    applyDisplayNow(clockView);
                    getHandler().postDelayed(this, UPDATE_INTERVAL_MS);
                }
            }
        };
        getHandler().postDelayed(poller, UPDATE_INTERVAL_MS);
    }

    private void applyDisplayNow(TextView clockView) {
        SimpleDateFormat timeFormat = mManaged.get(clockView);
        if (timeFormat == null) {
            return;
        }
        try {
            Context context = clockView.getContext();
            int mode = readModeFromFile();

            String time = timeFormat.format(new Date());
            String ram = getRamInfo(context);
            String full = time + " " + ram;
            float targetWidthPx = clockView.getPaint().measureText(full);

            String display;
            switch (mode) {
                case MODE_TIME_ONLY:
                    display = padToWidth(clockView, time, targetWidthPx);
                    break;
                case MODE_RAM_ONLY:
                    display = padToWidth(clockView, ram, targetWidthPx);
                    break;
                case MODE_TIME_RAM:
                default:
                    display = full;
                    break;
            }

            mApplyingOurText = true;
            try {
                clockView.setText(display);
            } finally {
                mApplyingOurText = false;
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": 更新文字出错: " + t);
        }
    }

    private String padToWidth(TextView view, String content, float targetWidthPx) {
        android.graphics.Paint paint = view.getPaint();
        float contentWidthPx = paint.measureText(content);
        float diffPx = targetWidthPx - contentWidthPx;
        if (diffPx <= 0) {
            return content;
        }
        float nbspWidthPx = paint.measureText("\u00A0");
        if (nbspWidthPx <= 0) {
            return content;
        }
        int count = Math.round(diffPx / nbspWidthPx);
        StringBuilder sb = new StringBuilder(content);
        for (int i = 0; i < count; i++) {
            sb.append('\u00A0');
        }
        return sb.toString();
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
