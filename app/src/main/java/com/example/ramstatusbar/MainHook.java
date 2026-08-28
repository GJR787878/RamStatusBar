package com.example.ramstatusbar;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
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
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

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

    private void startManaging(final TextView clockView) {

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
         * 恢复为透明背景。
         */
        clockView.setBackground(null);

        clockView.setClickable(true);

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
                    timeFormat.format(new Date());

            String ram =
                    getRamInfo(context);

            String full =
                    time + " " + ram;

            float targetWidthPx =
                    clockView.getPaint()
                            .measureText(full);

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

            String display =
                    padToWidth(
                            clockView,
                            rawContent,
                            targetWidthPx
                    );

            float rawContentWidthPx =
                    clockView.getPaint()
                            .measureText(rawContent);

            float neededWidthPx =
                    Math.max(
                            targetWidthPx,
                            rawContentWidthPx
                    );

            ensureFixedWidth(
                    clockView,
                    neededWidthPx
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

    private void ensureFixedWidth(
            TextView clockView,
            float neededWidthPx) {

        try {

            float oneCharPx =
                    clockView.getPaint()
                            .measureText("0");

            int desired =
                    Math.round(
                            neededWidthPx + oneCharPx
                    );

            Integer current =
                    mFixedWidthPx.get(clockView);

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

                lp.width = desired;

                clockView.setLayoutParams(lp);
            }

        } catch (Throwable ignored) {
        }
    }

    private String padToWidth(
            TextView view,
            String content,
            float targetWidthPx) {

        android.graphics.Paint paint =
                view.getPaint();

        float contentWidthPx =
                paint.measureText(content);

        float diffPx =
                targetWidthPx - contentWidthPx;

        if (diffPx <= 0) {
            return content;
        }

        float nbspWidthPx =
                paint.measureText("\u00A0");

        if (nbspWidthPx <= 0) {
            return content;
        }

        int count =
                Math.round(
                        diffPx / nbspWidthPx
                );

        StringBuilder sb =
                new StringBuilder(content);

        for (int i = 0; i < count; i++) {

            sb.append('\u00A0');
        }

        return sb.toString();
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

            for (File zoneDir : zones) {

                if (!zoneDir.getName()
                        .startsWith("thermal_zone")) {

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
