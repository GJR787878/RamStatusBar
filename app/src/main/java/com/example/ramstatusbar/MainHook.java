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

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "RamStatusBar";

    private static final String CLOCK_CLASS =
            "com.android.systemui.statusbar.policy.Clock";

    private static final String CONFIG_FILE =
            "/data/local/tmp/ramstatusbar_mode";

    private static final String CPU_FILE =
            "/data/local/tmp/ramstatusbar_cpu";

    /*
     * 与 MainActivity 共用的颜色设置。
     */
    private static final String UI_PREFS_NAME = "ui_prefs";
    private static final String KEY_BG_COLOR = "bg_color";

    /*
     * 默认背景颜色。
     *
     * 这里是半透明黑色。
     * 如果用户在颜色选择界面设置了颜色，
     * 就会使用用户选择的颜色。
     */
    private static final int DEFAULT_BG_COLOR = 0x99000000;

    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    private static final int TAP_NORMAL = 0;
    private static final int TAP_CPU = 1;
    private static final int TAP_GPU = 2;

    private static final long AUTO_REVERT_MS = 10000;
    private static final long UPDATE_INTERVAL_MS = 1000;

    private static final int[] COMMON_RAM_TIERS_GB = {
            3, 4, 6, 8, 12, 16, 18, 24, 32
    };

    /*
     * 胶囊左右各增加约半个字符宽度。
     */
    private static final float CAPSULE_HORIZONTAL_PADDING_CHARS = 0.5f;

    private Handler mHandler;

    private final Map<TextView, SimpleDateFormat> mManaged =
            new HashMap<>();

    private final Map<TextView, Integer> mTapState =
            new HashMap<>();

    private final Map<TextView, Runnable> mRevertRunnables =
            new HashMap<>();

    private final Map<TextView, Integer> mFixedWidthPx =
            new HashMap<>();

    private boolean mApplyingOurText = false;

    private Integer mLastCpuPercent = null;
    private Integer mLastGpuPercent = null;

    private Double mLastCpuTempC = null;
    private Double mLastGpuTempC = null;

    private List<File> mCpuTempZones = null;
    private List<File> mGpuTempZones = null;

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    @Override
    public void handleLoadPackage(
            XC_LoadPackage.LoadPackageParam lpparam) {

        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        try {

            Class<?> clockClass = Class.forName(
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
                                        (TextView) param.thisObject;

                                startManaging(tv);

                            } catch (Throwable t) {

                                XposedBridge.log(
                                        TAG +
                                        ": 构造后处理出错: " +
                                        t
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
                                        (TextView) param.thisObject;

                                if (mManaged.containsKey(tv)) {
                                    applyDisplayNow(tv);
                                }

                            } catch (Throwable ignored) {
                            }
                        }
                    }
            );

            XposedBridge.log(
                    TAG +
                    ": hook 安装成功 -> " +
                    CLOCK_CLASS
            );

        } catch (Throwable t) {

            XposedBridge.log(
                    TAG +
                    ": hook 安装失败: " +
                    t
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
         * 去掉系统原本的背景。
         * 后面由我们自己绘制胶囊。
         */
        clockView.setBackground(null);

        clockView.setClickable(true);

        /*
         * 点击：
         *
         * 第一次：CPU
         * 第二次：GPU
         * 第三次：正常
         */
        clockView.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        Integer cur =
                                mTapState.get(clockView);

                        int next =
                                ((cur == null
                                        ? TAP_NORMAL
                                        : cur) + 1) % 3;

                        mTapState.put(
                                clockView,
                                next
                        );

                        applyDisplayNow(clockView);

                        scheduleAutoRevert(clockView);
                    }
                }
        );

        applyDisplayNow(clockView);

        Runnable poller = new Runnable() {

            @Override
            public void run() {

                if (mManaged.containsKey(clockView)) {

                    applyDisplayNow(clockView);

                    getHandler().postDelayed(
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

        Runnable prev =
                mRevertRunnables.remove(clockView);

        if (prev != null) {
            getHandler().removeCallbacks(prev);
        }

        Runnable revert = new Runnable() {

            @Override
            public void run() {

                mTapState.put(
                        clockView,
                        TAP_NORMAL
                );

                applyDisplayNow(clockView);

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
                mManaged.get(clockView);

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
                    getRamInfo(context);

            String full =
                    time + " " + ram;

            Integer tapState =
                    mTapState.get(clockView);

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

                        rawContent = time;
                        break;

                    case MODE_RAM_ONLY:

                        rawContent = ram;
                        break;

                    case MODE_TIME_RAM:
                    default:

                        rawContent = full;
                        break;
                }
            }

            /*
             * 胶囊宽度直接根据当前显示文字计算。
             *
             * 这样：
             *
             * 21:11 3.5G/8G
             *
             * CPU35% 42°C
             *
             * GPU67% 39°C
             *
             * 都会自动适配。
             */
            float contentWidthPx =
                    clockView.getPaint()
                            .measureText(rawContent);

            float charWidthPx =
                    clockView.getPaint()
                            .measureText("0");

            float horizontalPadding =
                    charWidthPx *
                    CAPSULE_HORIZONTAL_PADDING_CHARS;

            float desiredWidth =
                    contentWidthPx +
                    horizontalPadding * 2.0f;

            ensureFixedWidth(
                    clockView,
                    desiredWidth
            );

            /*
             * 设置胶囊背景。
             */
            applyCapsuleBackground(
                    clockView
            );

            /*
             * 用不可见占位字符让 TextView 内部内容
             * 保持在胶囊的有效区域内。
             *
             * 这里只保留极少的左右空间，
             * 不再像之前一样额外扩大很多。
             */
            String display =
                    addHorizontalSpacing(
                            clockView,
                            rawContent
                    );

            mApplyingOurText = true;

            try {

                clockView.setText(display);

            } finally {

                mApplyingOurText = false;
            }

        } catch (Throwable t) {

            XposedBridge.log(
                    TAG +
                    ": 更新文字出错: " +
                    t
            );
        }
    }

    private String addHorizontalSpacing(
            TextView view,
            String content) {

        /*
         * 这里不使用普通空格，
         * 避免 Android 对状态栏文字进行额外处理。
         */
        return "\u00A0"
                + content
                + "\u00A0";
    }

    private void ensureFixedWidth(
            TextView clockView,
            float neededWidthPx) {

        try {

            int desired =
                    Math.max(
                            1,
                            Math.round(
                                    neededWidthPx
                            )
                    );

            Integer current =
                    mFixedWidthPx.get(clockView);

            /*
             * 当前宽度一样时不重复设置 LayoutParams，
             * 避免状态栏频繁重新布局。
             */
            if (current != null
                    && current == desired) {

                return;
            }

            mFixedWidthPx.put(
                    clockView,
                    desired
            );

            ViewGroup.LayoutParams lp =
                    clockView.getLayoutParams();

            if (lp != null) {

                lp.width = desired;

                clockView.setLayoutParams(lp);
            }

        } catch (Throwable ignored) {
        }
    }

    private void applyCapsuleBackground(
            TextView clockView) {

        try {

            int color =
                    getBackgroundColor(
                            clockView.getContext()
                    );

            GradientDrawable drawable =
                    new GradientDrawable();

            drawable.setColor(color);

            /*
             * 使用非常大的圆角，
             * Android 会自动形成胶囊形。
             */
            drawable.setCornerRadius(
                    100000.0f
            );

            /*
             * 不使用边框。
             */
            drawable.setStroke(
                    0,
                    Color.TRANSPARENT
            );

            clockView.setBackground(
                    drawable
            );

            /*
             * 背景已经由我们控制，
             * 所以让文字本身不产生额外背景。
             */
            clockView.setClipToOutline(false);

        } catch (Throwable t) {

            XposedBridge.log(
                    TAG +
                    ": 设置胶囊背景失败: " +
                    t
            );
        }
    }

    private int getBackgroundColor(
            Context context) {

        try {

            android.content.SharedPreferences prefs =
                    context.getSharedPreferences(
                            UI_PREFS_NAME,
                            Context.MODE_PRIVATE
                    );

            return prefs.getInt(
                    KEY_BG_COLOR,
                    DEFAULT_BG_COLOR
            );

        } catch (Throwable t) {

            return DEFAULT_BG_COLOR;
        }
    }

    private int readModeFromFile() {

        try {

            File f =
                    new File(CONFIG_FILE);

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

        am.getMemoryInfo(info);

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

        for (int tier : COMMON_RAM_TIERS_GB) {

            if (rawTotalGb <= tier + 0.5) {
                return tier;
            }
        }

        return (int)
                Math.round(rawTotalGb);
    }

    private String getCpuUsageString() {

        Integer percent =
                tryReadCpuPercent();

        if (percent != null) {
            mLastCpuPercent = percent;
        }

        String percentPart =
                mLastCpuPercent == null
                        ? "N/A"
                        : mLastCpuPercent + "%";

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
            mLastCpuTempC = tempC;
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
                    new File(CPU_FILE);

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
            mLastGpuPercent = percent;
        }

        String percentPart =
                mLastGpuPercent == null
                        ? "N/A"
                        : mLastGpuPercent + "%";

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
            mLastGpuTempC = tempC;
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

            for (File zoneDir : zones) {

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

                    if (t.startsWith(namePrefix)
                            && t.endsWith("-usr")) {

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

        Double max = null;

        if (tempFiles == null) {
            return null;
        }

        for (File f : tempFiles) {

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
                        "/sys/class/kgsl/kgsl-3d0/" +
                        "gpu_busy_percentage"
                );

        if (percent == null) {

            percent =
                    tryReadBusyRatioFile(
                            "/sys/class/kgsl/kgsl-3d0/" +
                            "gpubusy"
                    );
        }

        if (percent == null) {

            percent =
                    tryReadPercentageFile(
                            "/sys/class/misc/mali0/" +
                            "device/utilization"
                    );
        }

        if (percent == null) {

            percent =
                    tryReadPercentageFile(
                            "/sys/devices/platform/" +
                            "mali.0/utilization"
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
                    Long.parseLong(parts[0]);

            long total =
                    Long.parseLong(parts[1]);

            if (total <= 0) {
                return null;
            }

            return (int)
                    Math.round(
                            busy * 100.0 / total
                    );

        } catch (Throwable t) {

            return null;
        }
    }
}
