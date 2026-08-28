package com.example.ramstatusbar;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
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
     * 背景颜色配置文件。
     *
     * 格式：
     * 例如 FF000000
     *
     * 前两位 = Alpha
     * 后六位 = RGB
     */
    private static final String COLOR_FILE =
            "/data/local/tmp/ramstatusbar_color";

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
     * 默认背景：
     *
     * 00FFFFFF = 完全透明
     *
     * 以后 MainActivity 修改 COLOR_FILE 后，
     * MainHook 会自动读取新的颜色。
     */
    private static final int DEFAULT_BACKGROUND_COLOR =
            Color.TRANSPARENT;

    /*
     * 胶囊左右内边距。
     *
     * 不使用过大的固定宽度，
     * 而是根据文字实际宽度计算。
     */
    private static final float HORIZONTAL_PADDING_DP = 6.0f;

    /*
     * 胶囊上下内边距。
     */
    private static final float VERTICAL_PADDING_DP = 2.0f;

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
                                        (TextView)
                                                param.thisObject;

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
         * 点击本身仍然由 Clock 处理。
         */
        clockView.setClickable(true);

        /*
         * 创建胶囊背景。
         *
         * 颜色为透明时，
         * 胶囊本身完全透明。
         */
        applyCapsuleBackground(clockView);

        clockView.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

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

        applyDisplayNow(clockView);

        Runnable poller =
                new Runnable() {

                    @Override
                    public void run() {

                        if (mManaged.containsKey(
                                clockView)) {

                            applyDisplayNow(
                                    clockView
                            );

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
                mRevertRunnables.remove(
                        clockView
                );

        if (prev != null) {

            getHandler().removeCallbacks(
                    prev
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

            String full =
                    time + " " + ram;

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
             * 胶囊的实际宽度：
             *
             * 文字宽度
             * +
             * 左右 padding
             *
             * 不再人为增加一个完整字符。
             */
            float textWidth =
                    clockView.getPaint()
                            .measureText(
                                    rawContent
                            );

            float padding =
                    dpToPx(
                            clockView.getContext(),
                            HORIZONTAL_PADDING_DP
                    );

            float neededWidth =
                    textWidth +
                    padding * 2.0f;

            /*
             * 高度使用文字高度 + 上下 padding。
             */
            float verticalPadding =
                    dpToPx(
                            clockView.getContext(),
                            VERTICAL_PADDING_DP
                    );

            int desiredHeight =
                    calculateDesiredHeight(
                            clockView,
                            verticalPadding
                    );

            ensureFixedWidth(
                    clockView,
                    neededWidth
            );

            ensureFixedHeight(
                    clockView,
                    desiredHeight
            );

            /*
             * 不再用大量 NBSP 填充。
             *
             * 这样文字不会因为空格造成背景继续变长。
             */
            mApplyingOurText = true;

            try {

                clockView.setText(
                        rawContent
                );

            } finally {

                mApplyingOurText = false;
            }

            /*
             * 每次刷新都检查颜色，
             * 这样 MainActivity 修改颜色后，
             * 最多约 1 秒即可同步到状态栏。
             */
            applyCapsuleBackground(
                    clockView
            );

        } catch (Throwable t) {

            XposedBridge.log(
                    TAG +
                    ": 更新文字出错: " +
                    t
            );
        }
    }

    private int calculateDesiredHeight(
            TextView clockView,
            float verticalPadding) {

        try {

            android.graphics.Paint.FontMetrics fm =
                    clockView.getPaint()
                            .getFontMetrics();

            float textHeight =
                    fm.bottom - fm.top;

            return Math.round(
                    textHeight +
                    verticalPadding * 2.0f
            );

        } catch (Throwable ignored) {

            return clockView.getMeasuredHeight();
        }
    }

    private void ensureFixedWidth(
            TextView clockView,
            float neededWidthPx) {

        try {

            /*
             * 这里不再额外加一个字符。
             *
             * 背景只保留很小的左右 padding。
             */
            int desired =
                    Math.max(
                            1,
                            Math.round(
                                    neededWidthPx
                            )
                    );

            Integer current =
                    mFixedWidthPx.get(
                            clockView
                    );

            /*
             * 只在需要变大时调整，
             * 防止 SystemUI 不断触发布局。
             */
            if (current != null
                    && desired <= current) {

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

                clockView.setLayoutParams(
                        lp
                );
            }

        } catch (Throwable ignored) {
        }
    }

    private void ensureFixedHeight(
            TextView clockView,
            int desiredHeightPx) {

        try {

            if (desiredHeightPx <= 0) {
                return;
            }

            ViewGroup.LayoutParams lp =
                    clockView.getLayoutParams();

            if (lp == null) {
                return;
            }

            /*
             * 不强行缩小系统原本高度，
             * 只保证胶囊不会因为文字被裁剪。
             */
            if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT
                    || lp.height <= 0) {

                lp.height =
                        desiredHeightPx;

                clockView.setLayoutParams(
                        lp
                );
            }

        } catch (Throwable ignored) {
        }
    }

    private void applyCapsuleBackground(
            TextView clockView) {

        try {

            int color =
                    readBackgroundColor();

            /*
             * GradientDrawable 的圆角半径
             * 设为高度的一半，
             * 即得到真正的胶囊形状。
             */
            int height =
                    clockView.getMeasuredHeight();

            if (height <= 0) {

                height =
                        Math.round(
                                dpToPx(
                                        clockView.getContext(),
                                        24
                                )
                        );
            }

            float radius =
                    height / 2.0f;

            GradientDrawable background =
                    new GradientDrawable();

            background.setColor(
                    color
            );

            background.setCornerRadius(
                    radius
            );

            /*
             * 胶囊内部再设置一点左右空间。
             */
            int horizontalPadding =
                    Math.round(
                            dpToPx(
                                    clockView.getContext(),
                                    HORIZONTAL_PADDING_DP
                            )
                    );

            int verticalPadding =
                    Math.round(
                            dpToPx(
                                    clockView.getContext(),
                                    VERTICAL_PADDING_DP
                            )
                    );

            /*
             * 保存 TextView 原有 padding
             * 不太安全，因此这里只设置
             * background 本身。
             */
            clockView.setBackground(
                    background
            );

            /*
             * 保证文字在胶囊内部。
             */
            clockView.setGravity(
                    Gravity.CENTER
            );

        } catch (Throwable t) {

            XposedBridge.log(
                    TAG +
                    ": 设置胶囊背景失败: " +
                    t
            );
        }
    }

    private float dpToPx(
            Context context,
            float dp) {

        return dp *
                context.getResources()
                        .getDisplayMetrics()
                        .density;
    }

    private int readBackgroundColor() {

        try {

            File f =
                    new File(
                            COLOR_FILE
                    );

            if (!f.exists()) {

                return DEFAULT_BACKGROUND_COLOR;
            }

            BufferedReader br =
                    new BufferedReader(
                            new FileReader(f)
                    );

            String line =
                    br.readLine();

            br.close();

            if (line == null) {

                return DEFAULT_BACKGROUND_COLOR;
            }

            line =
                    line.trim();

            if (line.startsWith("#")) {

                line =
                        line.substring(1);
            }

            /*
             * 支持：
             *
             * RRGGBB
             * AARRGGBB
             */
            long value =
                    Long.parseLong(
                            line,
                            16
                    );

            if (line.length() == 6) {

                return (int)
                        (0xFF000000L |
                                value);
            }

            if (line.length() == 8) {

                return (int) value;
            }

        } catch (Throwable ignored) {
        }

        return DEFAULT_BACKGROUND_COLOR;
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
                    ) && t.endsWith(
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
                            busy * 100.0 /
                            total
                    );

        } catch (Throwable t) {

            return null;
        }
    }
}
