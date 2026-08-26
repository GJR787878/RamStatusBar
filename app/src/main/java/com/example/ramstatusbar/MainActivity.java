package com.example.ramstatusbar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String CONFIG_FILE = "/data/local/tmp/ramstatusbar_mode";

    private static final int MODE_TIME_ONLY = 0;
    private static final int MODE_TIME_RAM = 1;
    private static final int MODE_RAM_ONLY = 2;

    private static final String UI_PREFS_NAME = "ui_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String LANG_ZH = "zh";
    private static final String LANG_EN = "en";

    private static final int LANG_BUTTON_WIDTH_DP = 72;

    private boolean mEnglish;
    private TextView mDeepSleepText;
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private Runnable mDeepSleepUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences uiPrefs = getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE);
        mEnglish = LANG_EN.equals(uiPrefs.getString(KEY_LANGUAGE, LANG_ZH));

        int currentMode = readCurrentModeOrDefault();

        FrameLayout frame = new FrameLayout(this);

        ScrollView scrollView = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(48, 96, 48, 220);

        TextView title = new TextView(this);
        title.setTextSize(18);
        title.setText(mEnglish ? "RAM Status Bar Display" : "RAM 状态栏显示");
        content.addView(title);

        TextView intro = new TextView(this);
        intro.setTextSize(14);
        intro.setPadding(0, 24, 0, 32);
        intro.setText(mEnglish
                ? "Pick a display mode below. Changes take effect within 1 second, "
                        + "no reboot needed."
                : "选择下面的显示模式，最多等 1 秒即可生效，不需要重启手机。");
        content.addView(intro);

        final RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        final RadioButton rbTimeOnly = new RadioButton(this);
        rbTimeOnly.setId(1001);
        rbTimeOnly.setText(mEnglish ? "Time only" : "仅显示时间");

        final RadioButton rbTimeRam = new RadioButton(this);
        rbTimeRam.setId(1002);
        rbTimeRam.setText(mEnglish
                ? "Time + RAM (e.g. 21:11 2.5G/8G)"
                : "时间 + 内存 (如 21:11 2.5G/8G)");

        final RadioButton rbRamOnly = new RadioButton(this);
        rbRamOnly.setId(1003);
        rbRamOnly.setText(mEnglish
                ? "RAM only (e.g. 2.5G/8G)"
                : "仅显示内存 (如 2.5G/8G)");

        radioGroup.addView(rbTimeOnly);
        radioGroup.addView(rbTimeRam);
        radioGroup.addView(rbRamOnly);
        content.addView(radioGroup);

        if (currentMode == MODE_TIME_ONLY) {
            radioGroup.check(rbTimeOnly.getId());
        } else if (currentMode == MODE_RAM_ONLY) {
            radioGroup.check(rbRamOnly.getId());
        } else {
            radioGroup.check(rbTimeRam.getId());
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int mode;
                if (checkedId == rbTimeOnly.getId()) {
                    mode = MODE_TIME_ONLY;
                } else if (checkedId == rbRamOnly.getId()) {
                    mode = MODE_RAM_ONLY;
                } else {
                    mode = MODE_TIME_RAM;
                }
                boolean ok = writeModeToFile(mode);
                if (!ok) {
                    Toast.makeText(MainActivity.this,
                            mEnglish
                                    ? "Write failed, please check root permission"
                                    : "写入失败，请检查是否已授予 root 权限",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        TextView tapFeatureTitle = new TextView(this);
        tapFeatureTitle.setTextSize(15);
        tapFeatureTitle.setPadding(0, 56, 0, 8);
        tapFeatureTitle.setText(mEnglish ? "Tap the clock to check CPU / GPU" : "点击时钟查看 CPU / GPU");
        content.addView(tapFeatureTitle);

        TextView tapFeatureBody = new TextView(this);
        tapFeatureBody.setTextSize(13);
        tapFeatureBody.setText(mEnglish
                ? "Tap the status bar clock: 1st tap shows CPU usage, 2nd tap shows "
                        + "GPU usage, 3rd tap returns to normal. If left untouched for "
                        + "10 seconds it automatically returns to normal as well.\n\n"
                        + "GPU usage relies on chip-specific sysfs paths and may show "
                        + "\"GPU N/A\" on some devices, depending on your chipset."
                : "点击状态栏的时钟：第 1 次点击显示 CPU 占用率，第 2 次点击显示 "
                        + "GPU 占用率，第 3 次点击回到正常显示；10 秒内不再点击也会"
                        + "自动回到正常显示。\n\n"
                        + "GPU 占用率依赖具体芯片的私有接口，部分设备上可能会显示"
                        + "\"GPU N/A\"，能否读取取决于你的芯片型号。");
        content.addView(tapFeatureBody);

        TextView deepSleepTitle = new TextView(this);
        deepSleepTitle.setTextSize(15);
        deepSleepTitle.setPadding(0, 56, 0, 8);
        deepSleepTitle.setText(mEnglish ? "Deep sleep" : "深度休眠");
        content.addView(deepSleepTitle);

        TextView deepSleepDesc = new TextView(this);
        deepSleepDesc.setTextSize(13);
        deepSleepDesc.setText(mEnglish
                ? "Shown below is how much of the time since boot the device has "
                        + "spent in deep sleep, same figure as in About Phone \u2014 no "
                        + "more need to dig through system settings to find it."
                : "下面显示的是开机以来设备处于深度休眠状态的时长和占比，跟"
                        + "\"关于本机\"里的数值一致，不用再去系统设置里翻找。");
        content.addView(deepSleepDesc);

        mDeepSleepText = new TextView(this);
        mDeepSleepText.setTextSize(14);
        mDeepSleepText.setPadding(0, 16, 0, 0);
        content.addView(mDeepSleepText);

        TextView setupTitle = new TextView(this);
        setupTitle.setTextSize(15);
        setupTitle.setPadding(0, 56, 0, 8);
        setupTitle.setText(mEnglish ? "Setup" : "安装说明");
        content.addView(setupTitle);

        TextView setupBody = new TextView(this);
        setupBody.setTextSize(13);
        setupBody.setText(mEnglish
                ? "1. In LSPosed / Vector Manager, enable this module and check the "
                        + "scope com.android.systemui.\n"
                        + "2. Reboot once after the first install for it to take effect.\n"
                        + "3. Switching the display mode above requires root; a "
                        + "permission prompt will appear the first time \u2014 please allow it.\n"
                        + "4. Total RAM is auto-detected and rounded up to the nearest "
                        + "common spec (8/12/16/24G, etc)."
                : "1. 到 LSPosed / Vector Manager 里，对本模块勾选作用域 "
                        + "com.android.systemui 并启用模块。\n"
                        + "2. 首次安装完成后需要重启一次手机才会生效。\n"
                        + "3. 切换上面的显示模式需要 root 权限，首次切换会弹出授权"
                        + "请求，请点击允许。\n"
                        + "4. 总内存会自动检测并取整到最接近的常见规格"
                        + "(8/12/16/24G等)。");
        content.addView(setupBody);

        scrollView.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Button langButton = new Button(this);
        langButton.setText(mEnglish ? "\u4e2d\u6587" : "EN");
        langButton.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE);
            String newLang = mEnglish ? LANG_ZH : LANG_EN;
            prefs.edit().putString(KEY_LANGUAGE, newLang).apply();
            recreate();
        });
        float density = getResources().getDisplayMetrics().density;
        int langButtonWidthPx = Math.round(LANG_BUTTON_WIDTH_DP * density);
        FrameLayout.LayoutParams langParams = new FrameLayout.LayoutParams(
                langButtonWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
        langParams.gravity = Gravity.BOTTOM | Gravity.END;
        langParams.setMargins(0, 0, 32, 32);
        frame.addView(langButton, langParams);

        setContentView(frame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startDeepSleepUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDeepSleepUpdates();
    }

    private void startDeepSleepUpdates() {
        stopDeepSleepUpdates();
        mDeepSleepUpdater = new Runnable() {
            @Override
            public void run() {
                mDeepSleepText.setText(formatDeepSleepLine());
                mUiHandler.postDelayed(this, 1000);
            }
        };
        mUiHandler.post(mDeepSleepUpdater);
    }

    private void stopDeepSleepUpdates() {
        if (mDeepSleepUpdater != null) {
            mUiHandler.removeCallbacks(mDeepSleepUpdater);
            mDeepSleepUpdater = null;
        }
    }

    private String formatDeepSleepLine() {
        long elapsed = SystemClock.elapsedRealtime();
        long awake = SystemClock.uptimeMillis();
        long deepSleepMs = elapsed - awake;
        int percent = elapsed > 0 ? (int) Math.round(deepSleepMs * 100.0 / elapsed) : 0;

        long totalSeconds = deepSleepMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format(Locale.getDefault(), "%d:%02d:%02d (%d%%)", hours, minutes, seconds, percent);
    }

    private int readCurrentModeOrDefault() {
        try {
            java.io.File f = new java.io.File(CONFIG_FILE);
            if (!f.exists()) {
                return MODE_TIME_RAM;
            }
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f));
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

    private boolean writeModeToFile(int mode) {
        try {
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(su.getOutputStream());
            os.writeBytes("echo " + mode + " > " + CONFIG_FILE + "\n");
            os.writeBytes("chmod 666 " + CONFIG_FILE + "\n");
            os.writeBytes("exit\n");
            os.flush();
            int result = su.waitFor();
            return result == 0;
        } catch (Throwable t) {
            return false;
        }
    }
                }
