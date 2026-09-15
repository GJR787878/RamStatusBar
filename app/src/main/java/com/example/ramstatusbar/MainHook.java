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
    private static final String TIME_CONFIG_FILE =
            "/data/local/tmp/ramstatusbar_time";

    /*
     * 手动胶囊宽度（像素）。0 或文件不存在 = 自动。
     * 由 MainActivity 的滑块写入，SystemUI 每秒轮询读取，
     * 因此调节后无需重启即可实时生效。
     */
    private static final String WIDTH_FILE =
            "/data/local/tmp/ramstatusbar_width";

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
     * 极限收紧到二十分之一字符，仅保留文字不贴
     * 胶囊圆角的最小缓冲，最大化给旁边内容让空间。
     */
    private static final float
            CAPSULE_PADDING_CHARS = 0.05f;

    private Handler mHandler;

    private final Map<TextView, SimpleDateFormat>
            mManaged = new HashMap<>();

    private final Map<TextView, Integer>
            mTapState = new HashMap<>();

    private final Map<TextView, Runnable>
            mRevertRunnables = new HashMap<>();

    private final Map<TextView, Integer>
            mFixedWidthPx = new HashMap<>();

    /*
     * 宽度是否已真正应用到 LayoutParams。
     * 构造阶段视图未挂载，LayoutParams 为 null，
     * 挂载后需要重试一次，否则宽度永远不会生效。
     */
    private final Map<TextView, Boolean>
            mWidthApplied = new HashMap<>();

    /*
     * 每种视图的"所有可能内容最大宽度"缓存（只计算一次）。
     */
    private final Map<TextView, Float>
            mMaxWidthPx = new HashMap<>();

    private boolean mApplyingOurText =
            false;
    // 时间配置缓存
    private long mTimeConfigLastRead = 0;
    private boolean mTimeAutoSync = true;
    private String mTimeZoneId = null;
    private long mSyncTimeBase = 0;
    private long mSyncElapsedRealtime = 0;
    private long mCustomTime = 0;
    // 同步瞬间的系统时钟（UTC毫秒），优先用它消除硬件时钟漂移
    private long mSyncSystemTime = 0;

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

        /*
         * 文字整体靠左对齐（垂直保持居中），
         * 不再水平居中。
         */
        clockView.setGravity(
                Gravity.LEFT
                        | Gravity.CENTER_VERTICAL
        );

        /*
         * 关闭字体行高自带 padding，
         * 让垂直居中更精确（避免文字视觉上偏上）。
         */
        clockView.setIncludeFontPadding(false);

        /*
         * 左右各保留半个字符的胶囊内边距，
         * 文字靠左但不会贴到胶囊圆角上。
         */
        float halfCharPx =
                clockView.getPaint()
                        .measureText("0")
                        * CAPSULE_PADDING_CHARS;

        clockView.setPadding(
                Math.round(halfCharPx),
                0,
                Math.round(halfCharPx),
                0
        );

        /*
         * 强制单行、禁用省略号：
         * 即便内容临时超宽也绝不换行/出框，
         * 保证文字永远只在背景内水平靠左。
         */
        clockView.setSingleLine(true);
        clockView.setEllipsize(null);

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

            // 根据时间配置计算显示时间（NTP同步/自定义/系统时间）
            long displayTimeMs = getDisplayTime();
            java.util.TimeZone displayTz = getDisplayTimeZone();
            timeFormat.setTimeZone(displayTz);
            String time =
                    timeFormat.format(
                            new Date(displayTimeMs)
                    );
            XposedBridge.log(TAG + ": 显示时间=" + time 
                    + " 时区=" + displayTz.getID() 
                    + " UTC毫秒=" + displayTimeMs);

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

            float oneCharWidth =
                    clockView.getPaint()
                            .measureText("0");

            /*
             * 固定胶囊宽度 = 所有可能出现内容的精确最大宽度
             * （时间/内存/CPU/GPU 逐一测量，1 与 0 等不同数字
             * 的实际显示宽度不同，不能只按当前内容估算）。
             */
            float maxWidth =
                    getMaxContentWidthPx(
                            clockView,
                            ram
                    );

            /*
             * 胶囊左右增加半个字符。
             */
            float padding =
                    oneCharWidth
                            * CAPSULE_PADDING_CHARS;

            float neededWidth =
                    maxWidth
                            + padding * 2;

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

    /*
     * 读取手动胶囊宽度（像素）。0 或文件不存在 = 自动。
     * 每秒轮询时调用，天然实时（无需重启）。
     */
    private int readManualWidthPx() {

        try {

            File f =
                    new File(
                            WIDTH_FILE
                    );

            if (!f.exists()) {

                return 0;
            }

            BufferedReader br =
                    new BufferedReader(
                            new FileReader(f)
                    );

            String line =
                    br.readLine();

            br.close();

            if (line == null) {

                return 0;
            }

            int value =
                    Integer.parseInt(
                            line.trim()
                    );

            /*
             * 0 或负数 = 自动；上限 600px 防呆。
             */
            if (value <= 0) {

                return 0;
            }

            return Math.min(
                    value,
                    600
            );

        } catch (Throwable t) {

            return 0;
        }
    }

    private void ensureFixedWidth(
            TextView clockView,
            float neededWidthPx) {

        try {

            int manual =
                    readManualWidthPx();

            float oneCharPx =
                    clockView.getPaint()
                            .measureText("0");

            int desired;

            if (manual > 0) {

                /*
                 * 手动模式：完全使用滑块设定的宽度。
                 */
                desired =
                        manual;

            } else {

                /*
                 * 自动模式：neededWidth 已包含左右各半个字符的
                 * 胶囊内边距，再整体减掉半个字符让胶囊更紧凑，
                 * 给右边"最近应用"等内容让出空间。
                 */
                desired =
                        Math.round(
                                neededWidthPx
                                        - oneCharPx * 0.5f
                        );

                if (desired < 1) {

                    desired = 1;
                }
            }

            Integer current =
                    mFixedWidthPx.get(
                            clockView
                    );

            Boolean applied =
                    mWidthApplied.get(
                            clockView
                    );

            /*
             * 自动模式：宽度只扩大，不频繁缩小，
             * 防止状态栏因为 CPU/GPU 数字变化不断抖动。
             *
             * 手动模式：跳过该限制，每次强制应用，
             * 让滑块调节（包括调小）实时生效。
             */
            if (manual <= 0
                    && current != null
                    && desired <= current
                    && Boolean.TRUE.equals(applied)) {

                return;
            }

            mFixedWidthPx.put(
                    clockView,
                    desired
            );

            /*
             * 关键修复：构造阶段视图尚未挂载到父布局，
             * getLayoutParams() 返回 null，宽度永远不会生效；
             * 挂载后必须重试一次才能真正应用。
             */
            android.view.ViewGroup.LayoutParams lp =
                    clockView.getLayoutParams();

            if (lp != null) {

                lp.width =
                        mFixedWidthPx.get(
                                clockView
                        );

                clockView.setLayoutParams(
                        lp
                );

                /*
                 * 文字靠左：左右保留半个字符内边距，
                 * 避免文字贴到胶囊圆角。
                 */
                int padPx =
                        Math.round(
                                oneCharPx
                                        * CAPSULE_PADDING_CHARS
                        );

                clockView.setPadding(
                        padPx,
                        0,
                        padPx,
                        0
                );

                mWidthApplied.put(
                        clockView,
                        Boolean.TRUE
                );
            }

        } catch (Throwable ignored) {
        }
    }

    /*
     * 返回该视图"所有可能出现内容"的精确最大宽度，
     * 每种视图只计算一次并缓存。
     *
     * 内存上限按本机实际总内存（如 8G 设备就是 0.0~8.0G/8G），
     * 不会为 12/16/24G 等更大规格预留多余宽度，避免胶囊太长
     * 挤压右边的内容。
     */
    private float getMaxContentWidthPx(
            TextView clockView,
            String ram) {

        Float cached =
                mMaxWidthPx.get(
                        clockView
                );

        if (cached != null) {

            return cached;
        }

        float width =
                computeMaxContentWidth(
                        clockView,
                        ram
                );

        mMaxWidthPx.put(
                clockView,
                width
        );

        return width;
    }

    /*
     * 从内存字符串中解析总内存（GB）。
     * 例如 "2.8G/8G" → 8。
     */
    private int parseTotalGb(
            String ram) {

        try {

            int slash =
                    ram.indexOf(
                            '/'
                    );

            if (slash >= 0) {

                String totalStr =
                        ram.substring(
                                        slash + 1
                                )
                                .replace(
                                        "G",
                                        ""
                                )
                                .trim();

                return Integer.parseInt(
                        totalStr
                );
            }

        } catch (Throwable ignored) {
        }

        return 8;
    }

    /*
     * 精确计算所有可能出现内容的最大宽度。
     *
     * G / : / . 等字符是固定的，只有数字会变化；
     * 非等宽字体下不同数字（如 1 与 0）实际显示宽度不同，
     * 因此逐一测量所有真实可能出现的组合，取最宽值。
     *
     * 内存上限 = 本机实际总内存（自动检测），
     * 温度上限 100°C，胶囊只为本机规格预留宽度。
     */
    private float computeMaxContentWidth(
            TextView clockView,
            String ram) {

        android.graphics.Paint paint =
                clockView.getPaint();

        float max = 0f;
        String widestTime = "00:00";
        String widestRam = ram;

        // 1) 时间 HH:mm：一天全部 1440 种组合
        for (int h = 0; h < 24; h++) {

            for (int m = 0; m < 60; m++) {

                String t =
                        String.format(
                                Locale.US,
                                "%02d:%02d",
                                h,
                                m
                        );

                float w =
                        paint.measureText(
                                t
                        );

                if (w > max) {

                    max = w;
                    widestTime = t;
                }
            }
        }

        // 2) 内存 X.XG/XXG：可用内存 0.0 ~ 本机总内存，
        //    总内存自动检测（如 8G 设备就是 8G）
        int totalGb =
                parseTotalGb(
                        ram
                );

        for (int i = 0; i <= totalGb * 10; i++) {

            double avail =
                    i / 10.0;

            String r =
                    String.format(
                            Locale.getDefault(),
                            "%.1fG/%dG",
                            avail,
                            totalGb
                    );

            float w =
                    paint.measureText(
                            r
                    );

            if (w > max) {

                max = w;
                widestRam = r;
            }
        }

        // 3) 时间 + 内存 组合（以空格分隔，直接拼接测量）
        float combined =
                paint.measureText(
                        widestTime
                                + " "
                                + widestRam
                );

        if (combined > max) {

            max = combined;
        }

        // 4) CPU / GPU：占用率 0-100%，温度 0-100°C
        for (int p = 0; p <= 100; p++) {

            for (int t = 0; t <= 100; t++) {

                float w =
                        paint.measureText(
                                "CPU"
                                        + p
                                        + "% "
                                        + t
                                        + "°C"
                        );

                if (w > max) {

                    max = w;
                }

                w =
                        paint.measureText(
                                "GPU"
                                        + p
                                        + "% "
                                        + t
                                        + "°C"
                        );

                if (w > max) {

                    max = w;
                }
            }
        }

        return max;
    }

    // 读取时间配置（带缓存，每秒最多读一次）
    // 使用简单的key=value格式，避免JSON解析问题
    private void ensureTimeConfig() {
        long now = System.currentTimeMillis();
        if (now - mTimeConfigLastRead < 1000) {
            return;
        }
        mTimeConfigLastRead = now;
        try {
            java.io.File file = new java.io.File(TIME_CONFIG_FILE);
            if (!file.exists()) {
                return;
            }
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || !line.contains("=")) {
                    continue;
                }
                int eqIdx = line.indexOf("=");
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();
                if ("autoSync".equals(key)) {
                    mTimeAutoSync = "true".equals(value);
                } else if ("timeZone".equals(key)) {
                    mTimeZoneId = value;
                } else if ("customTime".equals(key)) {
                    try { mCustomTime = Long.parseLong(value); } catch (Throwable t) {}
                } else if ("syncTimeBase".equals(key)) {
                    try { mSyncTimeBase = Long.parseLong(value); } catch (Throwable t) {}
                } else if ("syncElapsedRealtime".equals(key)) {
                    try { mSyncElapsedRealtime = Long.parseLong(value); } catch (Throwable t) {}
                } else if ("syncSystemTime".equals(key)) {
                    try { mSyncSystemTime = Long.parseLong(value); } catch (Throwable t) {}
                }
            }
            br.close();
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": ensureTimeConfig读取失败: " + t.getMessage());
        }
        XposedBridge.log(TAG + ": 时间配置 autoSync=" + mTimeAutoSync 
                + " timeZone=" + mTimeZoneId 
                + " syncTimeBase=" + mSyncTimeBase 
                + " syncSystemTime=" + mSyncSystemTime
                + " customTime=" + mCustomTime);
    }

    // 根据时间配置计算当前显示时间（UTC毫秒）
    private long getDisplayTime() {
        ensureTimeConfig();
        if (mTimeAutoSync && mSyncTimeBase > 0 && mSyncSystemTime > 0) {
            // 以系统时钟为基准 + NTP 测得偏移量。
            // 系统时钟会被运营商/网络 NTP 持续校准，跟随它显示不会累积硬件时钟漂移，
            // 也不会在重启后因 elapsedRealtime 归零而跳错。
            return System.currentTimeMillis() + (mSyncTimeBase - mSyncSystemTime);
        }
        if (mTimeAutoSync && mSyncTimeBase > 0 && mSyncElapsedRealtime > 0) {
            // 兼容旧版本配置：仍使用一次性NTP锚点 + 硬件时钟投影（存在每日漂移）
            long elapsed = android.os.SystemClock.elapsedRealtime() - mSyncElapsedRealtime;
            return mSyncTimeBase + elapsed;
        }
        if (!mTimeAutoSync && mCustomTime > 0) {
            // 使用自定义时间（固定显示，不流逝）
            return mCustomTime;
        }
        // 默认使用系统时间
        return System.currentTimeMillis();
    }

    // 获取用于格式化的时区
    private java.util.TimeZone getDisplayTimeZone() {
        ensureTimeConfig();
        if (mTimeZoneId != null && !mTimeZoneId.isEmpty()) {
            try {
                return java.util.TimeZone.getTimeZone(mTimeZoneId);
            } catch (Throwable t) {
                // 忽略
            }
        }
        return java.util.TimeZone.getDefault();
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
