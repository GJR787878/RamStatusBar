package com.example.ramstatusbar;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook
        implements IXposedHookLoadPackage {

    private static final String TAG =
            "RamStatusBar";

    private static final String CLOCK_CLASS =
            "com.android.systemui.statusbar.policy.Clock";

    private static final String CONFIG_FILE =
            "/data/local/tmp/ramstatusbar_mode";

    private static final String CPU_FILE =
            "/data/local/tmp/ramstatusbar_cpu";

    /*
     * 和 ColorSettingsActivity 使用同一个文件。
     */
    private static final String COLOR_FILE =
            "/data/local/tmp/ramstatusbar_color";

    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    private static final int TAP_NORMAL = 0;
    private static final int TAP_CPU = 1;
    private static final int TAP_GPU = 2;

    private static final long AUTO_REVERT_MS =
            10000;

    private static final long UPDATE_INTERVAL_MS =
            1000;

    private static final int[] COMMON_RAM_TIERS_GB = {
            3, 4, 6, 8, 12, 16, 18, 24, 32
    };

    /*
     * 胶囊左右额外空间。
     *
     * 保留半个字符宽度的设计。
     */
    private static final float
            CAPSULE_PADDING_CHARS = 0.5f;

    private Handler mHandler;

    private final Map<TextView, SimpleDateFormat>
            mManaged = new HashMap<>();

    private final Map<TextView, Integer>
            mTapState = new HashMap<>();

    private final Map<TextView, Runnable>
            mRevertRunnables = new HashMap<>();

    private final Map<TextView, Integer>
            mFixedWidthPx = new HashMap<>();

    private boolean mApplyingOurText =
            false;

    private Integer mLastCpuPercent = null;
    private Integer mLastGpuPercent = null;

    private Double mLastCpuTempC = null;
    private Double mLastGpuTempC = null;

    private List<File> mCpuTempZones = null;
    private List<File> mGpuTempZones = null;

    private Handler getHandler() {

        if (mHandler == null) {

            mHandler =
                    new Handler(
                            Looper.getMainLooper()
                    );
        }

        return mHandler;
    }

    @Override
    public void handleLoadPackage(
            XC_LoadPackage.LoadPackageParam lpparam) {

        if (!"com.android.systemui".equals(
                lpparam.packageName)) {

            return;
        }

        try {

            Class<?> clockClass =
                    Class.forName(
                            CLOCK_CLASS,
                            false,
                            lpparam.classLoader
                    );

            XposedBridge.hookAllConstructors(
                    clockClass,
                    new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param) {

                            try {

                                TextView tv =
                                        (TextView)
                                                param.thisObject;

                                startManaging(tv);

                            } catch (Throwable t) {

                                XposedBridge.log(
                                        TAG
                                                + ": 构造后处理出错: "
                                                + t
                                );
                            }
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    TextView.class,
                    "setText",
                    CharSequence.class,
                    TextView.BufferType.class,

                    new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(
                                MethodHookParam param) {

                            try {

                                if (mApplyingOurText) {
                                    return;
                                }

                                TextView tv =
                                        (TextView)
                                                param.thisObject;

                                if (mManaged.containsKey(
                                        tv)) {

                                    applyDisplayNow(tv);
                                }

                            } catch (Throwable ignored) {
                            }
                        }
                    }
            );

            XposedBridge.log(
                    TAG
                            + ": hook 安装成功 -> "
                            + CLOCK_CLASS
            );

        } catch (Throwable t) {

            XposedBridge.log(
                    TAG
                            + ": hook 安装失败: "
                            + t
            );
        }
    }

    private void startManaging(
            final TextView clockView) {

        mManaged.put(
                clockView,
                new SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                )
        );

        mTapState.put(
                clockView,
                TAP_NORMAL
        );

        /*
         * 清除 SystemUI 原来的背景。
         */
        clockView.setBackground(null);

        clockView.setClickable(true);

        clockView.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(
                            View v) {

                        Integer cur =
                                mTapState.get(
                                        clockView
                                );

                        int next =
                                ((cur == null
                                        ? TAP_NORMAL
                                        : cur) + 1) % 3;

                        mTapState.put(
                                clockView,
                                next
                        );

                        applyDisplayNow(
                                clockView
                        );

                        scheduleAutoRevert(
                                clockView
                        );
                    }
                }
        );

        applyDisplayNow(
                clockView
        );

        Runnable poller =
                new Runnable() {

                    @Override
                    public void run() {

                        if (mManaged.containsKey(
                                clockView)) {

                            applyDisplayNow(
                                    clockView
                            );

                            getHandler()
                                    .postDelayed(
                                            this,
                                            UPDATE_INTERVAL_MS
                                    );
                        }
                    }
                };

        getHandler().postDelayed(
                poller,
                UPDATE_INTERVAL_MS
        );
    }

    private void scheduleAutoRevert(
            final TextView clockView) {

        Runnable previous =
                mRevertRunnables.remove(
                        clockView
                );

        if (previous != null) {

            getHandler()
                    .removeCallbacks(
                            previous
                    );
        }

        Runnable revert =
                new Runnable() {

                    @Override
                    public void run() {

                        mTapState.put(
                                clockView,
                                TAP_NORMAL
                        );

                        applyDisplayNow(
                                clockView
                        );

                        mRevertRunnables.remove(
                                clockView
                        );
                    }
                };

        mRevertRunnables.put(
                clockView,
                revert
        );

        getHandler().postDelayed(
                revert,
                AUTO_REVERT_MS
        );
    }

    private void applyDisplayNow(
            TextView clockView) {

        SimpleDateFormat timeFormat =
                mManaged.get(
                        clockView
                );

        if (timeFormat == null) {
            return;
        }

        try {

            Context context =
                    clockView.getContext();

            String time =
                    timeFormat.format(
                            new Date()
                    );

            String ram =
                    getRamInfo(
                            context
                    );

            String normalContent =
                    time
                            + " "
                            + ram;

            Integer tapState =
                    mTapState.get(
                            clockView
                    );

            String rawContent;

            if (tapState != null
                    && tapState == TAP_CPU) {

                rawContent =
                        getCpuUsageString();

            } else if (tapState != null
                    && tapState == TAP_GPU) {

                rawContent =
                        getGpuUsageString();

            } else {

                int mode =
                        readModeFromFile();

                switch (mode) {

                    case MODE_TIME_ONLY:

                        rawContent =
                                time;

                        break;

                    case MODE_RAM_ONLY:

                        rawContent =
                                ram;

                        break;

                    case MODE_TIME_RAM:
                    default:

                        rawContent =
                                normalContent;

                        break;
                }
            }

            float normalWidth =
                    clockView.getPaint()
                            .measureText(
                                    normalContent
                            );

            float contentWidth =
                    clockView.getPaint()
                            .measureText(
                                    rawContent
                            );

            float oneCharWidth =
                    clockView.getPaint()
                            .measureText("0");

            /*
             * 保证当前 CPU/GPU 文字也有足够宽度。
             */
            float baseWidth =
                    Math.max(
                            normalWidth,
                            contentWidth
                    );

            /*
             * 胶囊左右增加半个字符。
             */
            float padding =
                    oneCharWidth
                            * CAPSULE_PADDING_CHARS;

            float neededWidth =
                    baseWidth
                            + padding * 2;

            /*
             * 比以前的固定宽度少半个字符。
             */
            ensureFixedWidth(
                    clockView,
                    neededWidth
            );

            /*
             * 设置胶囊背景。
             */
            applyCapsuleBackground(
                    clockView
            );

            /*
             * 文字不额外添加 nbsp，
             * 避免文字被人为撑长。
             */
            mApplyingOurText = true;

            try {

                clockView.setText(
                        rawContent
                );

            } finally {

                mApplyingOurText = false;
            }

        } catch (Throwable t) {

            XposedBridge.log(
                    TAG
                            + ": 更新文字出错: "
                            + t
            );
        }
    }

    /*
     * 从 /data/local/tmp/ramstatusbar_color
     * 读取背景颜色。
     */
    private int readBackgroundColor() {

        try {

            File file =
                    new File(
                            COLOR_FILE
                    );

            if (!file.exists()) {

                return 0xCC000000;
            }

            BufferedReader br =
                    new BufferedReader(
                            new FileReader(
                                    file
                            )
                    );

            String line =
                    br.readLine();

            br.close();

            if (line == null) {

                return 0xCC000000;
            }

            line =
                    line.trim()
                            .replace(
                                    "#",
                                    ""
                            );

            if (line.isEmpty()) {

                return 0xCC000000;
            }

            long value =
                    Long.parseLong(
                            line,
                            16
                    );

            return (int) value;

        } catch (Throwable t) {

            return 0xCC000000;
        }
    }

    /*
     * 创建胶囊背景。
     */
    private void applyCapsuleBackground(
            TextView clockView) {

        try {

            int color =
                    readBackgroundColor();

            /*
             * 0x00000000 = 完全透明。
             */
            if (color == Color.TRANSPARENT) {

                clockView.setBackground(
                        null
                );

                return;
            }

            GradientDrawable drawable =
                    new GradientDrawable();

            drawable.setShape(
                    GradientDrawable.RECTANGLE
            );

            drawable.setColor(
                    color
            );

            /*
             * 极大圆角，
             * 让矩形变成胶囊。
             */
            drawable.setCornerRadius(
                    10000f
            );

            clockView.setBackground(
                    drawable
            );

        } catch (Throwable ignored) {
        }
    }

    private void ensureFixedWidth(
            TextView clockView,
            float neededWidthPx) {

        try {

            float oneCharPx =
                    clockView.getPaint()
                            .measureText("0");

            /*
             * 在需要宽度上增加左右空间，
             * 然后减掉半个字符，
             * 保留你之前要求的效果。
             */
            int desired =
                    Math.round(
                            neededWidthPx
                                    - oneCharPx * 0.5f
                    );

            if (desired < 1) {

                desired = 1;
            }

            Integer current =
                    mFixedWidthPx.get(
                            clockView
                    );

            /*
             * 宽度只扩大，不频繁缩小。
             *
             * 这样可以防止状态栏因为
             * CPU/GPU 数字变化不断抖动。
             */
            if (current != null
                    && desired <= current) {

                return;
            }

            mFixedWidthPx.put(
                    clockView,
                    desired
            );

            android.view.ViewGroup.LayoutParams lp =
                    clockView.getLayoutParams();

            if (lp != null) {

                lp.width =
                        desired;

                clockView.setLayoutParams(
                        lp
                );
            }

        } catch (Throwable ignored) {
        }
    }

    private int readModeFromFile() {

        try {

            File f =
                    new File(
                            CONFIG_FILE
                    );

            if (!f.exists()) {

                return MODE_TIME_RAM;
            }

            BufferedReader br =
                    new BufferedReader(
                            new FileReader(f)
                    );

            String line =
                    br.readLine();

            br.close();

            if (line == null) {

                return MODE_TIME_RAM;
            }

            return Integer.parseInt(
                    line.trim()
            );

        } catch (Throwable t) {

            return MODE_TIME_RAM;
        }
    }

    private String getRamInfo(
            Context context) {

        ActivityManager am =
                (ActivityManager)
                        context.getSystemService(
                                Context.ACTIVITY_SERVICE
                        );

        ActivityManager.MemoryInfo info =
                new ActivityManager.MemoryInfo();

        am.getMemoryInfo(
                info
        );

        double availGb =
                info.availMem
                        / 1024.0
                        / 1024.0
                        / 1024.0;

        double rawTotalGb =
                info.totalMem
                        / 1024.0
                        / 1024.0
                        / 1024.0;

        int totalGb =
                roundToCommonTier(
                        rawTotalGb
                );

        return String.format(
                Locale.getDefault(),
                "%.1fG/%dG",
                availGb,
                totalGb
        );
    }

    private int roundToCommonTier(
            double rawTotalGb) {

        for (int tier :
                COMMON_RAM_TIERS_GB) {

            if (rawTotalGb
                    <= tier + 0.5) {

                return tier;
            }
        }

        return (int)
                Math.round(
                        rawTotalGb
                );
    }

    private String getCpuUsageString() {

        Integer percent =
                tryReadCpuPercent();

        if (percent != null) {

            mLastCpuPercent =
                    percent;
        }

        String percentPart =
                mLastCpuPercent == null
                        ? "N/A"
                        : mLastCpuPercent
                                + "%";

        if (mCpuTempZones == null) {

            mCpuTempZones =
                    discoverThermalZones(
                            "cpuss-"
                    );
        }

        Double tempC =
                getMaxTempCelsius(
                        mCpuTempZones
                );

        if (tempC != null) {

            mLastCpuTempC =
                    tempC;
        }

        String tempPart =
                mLastCpuTempC == null
                        ? ""
                        : " "
                        + Math.round(
                                mLastCpuTempC
                        )
                        + "\u00b0C";

        return "CPU"
                + percentPart
                + tempPart;
    }

    private Integer tryReadCpuPercent() {

        try {

            File f =
                    new File(
                            CPU_FILE
                    );

            if (!f.exists()) {

                return null;
            }

            BufferedReader br =
                    new BufferedReader(
                            new FileReader(f)
                    );

            String line =
                    br.readLine();

            br.close();

            if (line == null) {

                return null;
            }

            return Integer.parseInt(
                    line.trim()
            );

        } catch (Throwable t) {

            return null;
        }
    }

    private String getGpuUsageString() {

        Integer percent =
                tryReadGpuPercentRaw();

        if (percent != null) {

            mLastGpuPercent =
                    percent;
        }

        String percentPart =
                mLastGpuPercent == null
                        ? "N/A"
                        : mLastGpuPercent
                                + "%";

        if (mGpuTempZones == null) {

            mGpuTempZones =
                    discoverThermalZones(
                            "gpuss-"
                    );
        }

        Double tempC =
                getMaxTempCelsius(
                        mGpuTempZones
                );

        if (tempC != null) {

            mLastGpuTempC =
                    tempC;
        }

        String tempPart =
                mLastGpuTempC == null
                        ? ""
                        : " "
                        + Math.round(
                                mLastGpuTempC
                        )
                        + "\u00b0C";

        return "GPU"
                + percentPart
                + tempPart;
    }

    private List<File> discoverThermalZones(
            String namePrefix) {

        List<File> result =
                new ArrayList<>();

        try {

            File thermalDir =
                    new File(
                            "/sys/class/thermal"
                    );

            File[] zones =
                    thermalDir.listFiles();

            if (zones == null) {

                return result;
            }

            for (File zoneDir :
                    zones) {

                if (!zoneDir.getName()
                        .startsWith(
                                "thermal_zone"
                        )) {

                    continue;
                }

                try {

                    File typeFile =
                            new File(
                                    zoneDir,
                                    "type"
                            );

                    BufferedReader br =
                            new BufferedReader(
                                    new FileReader(
                                            typeFile
                                    )
                            );

                    String type =
                            br.readLine();

                    br.close();

                    if (type == null) {

                        continue;
                    }

                    String t =
                            type.trim()
                                    .toLowerCase(
                                            Locale.US
                                    );

                    if (t.startsWith(
                            namePrefix
                    )
                            && t.endsWith(
                                    "-usr"
                            )) {

                        result.add(
                                new File(
                                        zoneDir,
                                        "temp"
                                )
                        );
                    }

                } catch (Throwable ignored) {
                }
            }

        } catch (Throwable ignored) {
        }

        return result;
    }

    private Double getMaxTempCelsius(
            List<File> tempFiles) {

        if (tempFiles == null) {

            return null;
        }

        Double max = null;

        for (File f :
                tempFiles) {

            try {

                BufferedReader br =
                        new BufferedReader(
                                new FileReader(f)
                        );

                String line =
                        br.readLine();

                br.close();

                if (line == null) {

                    continue;
                }

                int raw =
                        Integer.parseInt(
                                line.trim()
                        );

                double celsius =
                        raw / 1000.0;

                if (max == null
                        || celsius > max) {

                    max = celsius;
                }

            } catch (Throwable ignored) {
            }
        }

        return max;
    }

    private Integer tryReadGpuPercentRaw() {

        Integer percent =
                tryReadPercentageFile(
                        "/sys/class/kgsl/kgsl-3d0/"
                                + "gpu_busy_percentage"
                );

        if (percent == null) {

            percent =
                    tryReadBusyRatioFile(
                            "/sys/class/kgsl/kgsl-3d0/"
                                    + "gpubusy"
                    );
        }

        if (percent == null) {

            percent =
                    tryReadPercentageFile(
                            "/sys/class/misc/mali0/"
                                    + "device/utilization"
                    );
        }

        if (percent == null) {

            percent =
                    tryReadPercentageFile(
                            "/sys/devices/platform/"
                                    + "mali.0/utilization"
                    );
        }

        return percent;
    }

    private Integer tryReadPercentageFile(
            String path) {

        try {

            File f =
                    new File(path);

            if (!f.exists()) {

                return null;
            }

            BufferedReader br =
                    new BufferedReader(
                            new FileReader(f)
                    );

            String line =
                    br.readLine();

            br.close();

            if (line == null) {

                return null;
            }

            String numeric =
                    line.trim()
                            .replaceAll(
                                    "[^0-9]",
                                    ""
                            );

            if (numeric.isEmpty()) {

                return null;
            }

            int value =
                    Integer.parseInt(
                            numeric
                    );

            return Math.min(
                    value,
                    100
            );

        } catch (Throwable t) {

            return null;
        }
    }

    private Integer tryReadBusyRatioFile(
            String path) {

        try {

            File f =
                    new File(path);

            if (!f.exists()) {

                return null;
            }

            BufferedReader br =
                    new BufferedReader(
                            new FileReader(f)
                    );

            String line =
                    br.readLine();

            br.close();

            if (line == null) {

                return null;
            }

            String[] parts =
                    line.trim()
                            .split("\\s+");

            if (parts.length < 2) {

                return null;
            }

            long busy =
                    Long.parseLong(
                            parts[0]
                    );

            long total =
                    Long.parseLong(
                            parts[1]
                    );

            if (total <= 0) {

                return null;
            }

            return (int)
                    Math.round(
                            busy
                                    * 100.0
                                    / total
                    );

        } catch (Throwable t) {

            return null;
        }
    }
}
